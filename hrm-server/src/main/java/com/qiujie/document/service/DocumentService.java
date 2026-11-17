package com.qiujie.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.util.ObjectDownloadWriter;
import com.qiujie.document.entity.Document;
import com.qiujie.document.entity.DocumentChunk;
import com.qiujie.document.enums.DocumentStatusEnum;
import com.qiujie.document.mapper.DocumentChunkMapper;
import com.qiujie.document.mapper.DocumentMapper;
import com.qiujie.document.vo.DocumentVO;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 知识库文档服务——模块唯一的公共 API：查询（列表、切片、下载）与命令（提交、重试、删除）。
 * <p>
 * 命令分两类：写状态类（{@link #submit}/{@link #retry}）都是「CAS 认领 → 派发 ETL」，
 * 提交在事务内、ETL 挂在提交后；重试不挂事务，认领成功即派发。删除类
 * （{@link #delete}/{@link #deleteBatch}）不挂事务，删行后同步清理物理产物。
 * <p>
 * 只操作 {@code kb_document} 与 PG 的 kb_chunk，与 docs 模块互不 import。
 * <p>
 * 两个事务前提：{@link #submit} 的提交后回调依赖事务代理，命令方法不得互相调用；
 * 删除类要求调用方不包事务，否则 purge 先于外层提交执行、回滚后留下坏行
 * （当前唯一调用方 DocumentController 不包事务，新增调用方需同步调整）。
 */
@Service
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    @Qualifier("fileTaskExecutor")
    private Executor ingestExecutor;

    @Autowired
    private IngestionPipeline pipeline;

    @Autowired
    private MinioStorageService storageService;

    /** 分页查询：按原文件名模糊匹配、按状态过滤。 */
    public ResponseDTO list(Integer current, Integer size, String oldName, String status) {
        if (oldName == null) {
            oldName = "";
        }
        IPage<DocumentVO> config = new Page<>(current, size);
        IPage<DocumentVO> page = documentMapper.listDocumentVO(config, oldName, status);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    /** 查看某文档的全部切片。 */
    public ResponseDTO chunks(Long documentId) {
        List<DocumentChunk> chunks = chunkMapper.selectByDocumentIdOrderByChunkIndex(documentId);
        return Response.success(chunks);
    }

    /**
     * 提交上传文档：事务内 INSERT(UPLOADED) + CAS 认领，提交后异步 ETL。
     * <p>
     * 事务内调用（分片上传回调）加入外层事务，事务外调用自开事务。
     *
     * @param document name 必须是 MinIO 对象键，compressed 决定 ETL 是否解压；
     *                 status / uploadTime 由本方法覆盖
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Document document) {
        document.setStatus(DocumentStatusEnum.UPLOADED.name())
                .setUploadTime(LocalDateTime.now());
        documentMapper.insert(document);
        // 新行必是 UPLOADED，认领不会落空；保留是为了让两条命令路径都经由同一状态入口
        Long documentId = document.getId();
        documentMapper.claimForProcessing(documentId);
        // 必须等提交：worker 读不到未提交的行，抢跑会卡死在 PROCESSING
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestExecutor.execute(() -> pipeline.run(documentId));
            }
        });
    }

    /**
     * 重新摄入：不存在与 PROCESSING 拒绝，其余状态接受。
     * <p>
     * 不挂事务：源文件早已在 MinIO，认领是一条自动提交的 UPDATE，成功即可派发。
     * 认领同时清掉上一轮残留，在途 ETL 不会被抢走。
     */
    public ResponseDTO retry(Long documentId) {
        if (documentMapper.selectById(documentId) == null) {
            return Response.error("文档不存在或已删除");
        }
        if (documentMapper.claimForProcessing(documentId) == 0) {
            return Response.error("文档正在处理中，请等待本次处理结束");
        }
        ingestExecutor.execute(() -> pipeline.run(documentId));
        return Response.success("已重新提交处理");
    }

    /**
     * 删除文档：物理删行后同步清理 PG 切片与 MinIO 对象。幂等：行不存在也算成功。
     * <p>
     * 不挂事务——物理产物回滚不了，前提是调用方不包事务（见类头）。
     */
    public ResponseDTO delete(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || documentMapper.deleteById(documentId) == 0) {
            return Response.success();
        }
        // 对象键必须在删行前取出，行删除即失去线索
        purge(List.of(document));
        return Response.success();
    }

    /** 批量删除：一次查、一次删，随即统一清理物理产物。不挂事务，前提同 {@link #delete}。 */
    public ResponseDTO deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.success();
        }
        List<Document> documents = documentMapper.selectByIds(ids);
        if (documents.isEmpty()) {
            return Response.success();
        }
        documentMapper.deleteByIds(documents.stream().map(Document::getId).toList());
        purge(documents);
        return Response.success();
    }

    /** 下载原始文件（按 magic-byte 判断并流式解压）。 */
    public void download(String key, HttpServletResponse response) throws IOException {
        ObjectDownloadWriter.write(storageService, key, response);
    }

    /**
     * 清理已删除文档的物理产物：PG 切片/向量 → MinIO 物理文件，幂等。
     * 实体须由调用方在删行前取出，行删掉后对象键已无迹可寻。
     */
    private void purge(List<Document> documents) {
        List<Long> documentIds = documents.stream().map(Document::getId).toList();
        List<String> storageKeys = documents.stream()
                .map(Document::getName)
                .filter(Objects::nonNull)
                .toList();

        // PG 切片必须先删：残留切片会继续参与检索，失败要留痕（DocumentCleanupJob 次日兜底）
        try {
            chunkMapper.deleteByDocumentIds(documentIds);
        } catch (Exception e) {
            log.error("删除检索产物失败，切片将残留在 kb_chunk 中，需人工清理: documentIds={}",
                    documentIds, e);
        }

        // MinIO 远端调用失败有 DocumentCleanupJob 兜底，放在最后且只记日志
        try {
            Set<String> failed = storageService.deleteAll(storageKeys);
            if (!failed.isEmpty()) {
                log.warn("删除物理文件失败（对象将由孤儿清理兜底）: {}", failed);
            }
        } catch (Exception e) {
            log.warn("删除物理文件失败（对象将由孤儿清理兜底）: {}", storageKeys, e);
        }
    }
}
