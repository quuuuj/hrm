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
import com.qiujie.document.lifecycle.DocumentLifecycleService;
import com.qiujie.document.mapper.DocumentChunkMapper;
import com.qiujie.document.mapper.DocumentMapper;
import com.qiujie.document.vo.DocumentVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档服务：列表、切片查看、重试、删除、下载。
 * <p>
 * 只操作 {@code kb_document} 与 PG 的 kb_chunk；文件中心的 sys_docs 由 docs 模块负责，
 * 两个模块互不 import。
 */
@Service
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private DocumentLifecycleService lifecycle;

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

    /** 重新提交摄入：READY 与不存在的文档拒绝。 */
    public ResponseDTO retry(Long documentId) {
        DocumentLifecycleService.RetryResult r =
                lifecycle.retry(new DocumentLifecycleService.RetryCommand(documentId));
        return r.accepted() ? Response.success("已重新提交处理") : Response.error(r.reason());
    }

    /** 删除文档：事务内物理删行，提交后清理 MinIO 对象与 PG 切片。 */
    public ResponseDTO delete(Long documentId) {
        // 幂等：行已不存在也算成功
        lifecycle.delete(new DocumentLifecycleService.DeleteCommand(documentId));
        return Response.success();
    }

    public ResponseDTO deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            lifecycle.delete(new DocumentLifecycleService.DeleteCommand(id));
        }
        return Response.success();
    }

    /** 下载原始文件（按 magic-byte 判断并流式解压）。 */
    public void download(String key, HttpServletResponse response) throws IOException {
        ObjectDownloadWriter.write(storageService, key, response);
    }
}
