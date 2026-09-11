package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.Docs;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService;
import com.qiujie.spi.UploadCompletionHandler;
import com.qiujie.spi.UploadSessionInfo;
import com.qiujie.mapper.DocsMapper;
import com.qiujie.storage.MinioStorageService;
import com.qiujie.util.StorageCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件中心唯一上传完成处理器：SHA256 秒传 + zstd 压缩 + 写入 sys_docs。
 * <p>
 * 由 {@link UploadSessionInfo#isIngest()} 决定是否加入知识库：
 * ingest=true 时走 {@link DocumentLifecycleService#register} 登记（kb_status=UPLOADED，
 * 事务提交后异步 ETL）；ingest=false 时仅做通用文件存储。
 */
@Component
public class DocsUploadCompletionHandler implements UploadCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(DocsUploadCompletionHandler.class);

    @Autowired
    private DocsMapper docsMapper;

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DocumentLifecycleService lifecycle;

    @Override
    public String getStoragePrefix() {
        return "docs";
    }

    @Override
    public Map<String, Object> checkDedup(String fileHash) {
        // @TableLogic 自动过滤 is_deleted=1：秒传只命中存活文档（软删除文件不秒传）
        List<Docs> existing = docsMapper.selectList(
                new QueryWrapper<Docs>().eq("file_hash", fileHash));
        if (existing.isEmpty()) return null;
        Docs doc = existing.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("id", doc.getId());
        result.put("name", doc.getName());
        result.put("oldName", doc.getOldName());
        result.put("kbStatus", doc.getKbStatus());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> onComplete(String mergedKey, UploadSessionInfo session) {
        // 读取合并后的文件内容用于压缩
        byte[] rawBytes;
        try (java.io.InputStream in = storageService.get(mergedKey)) {
            rawBytes = in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("读取合并文件失败: " + mergedKey, e);
        }

        // zstd 压缩（ETL 读取时有 magic-byte 解压通道，统一压缩不影响摄入）
        StorageCompressor.CompressionResult compressed =
                StorageCompressor.tryCompress(rawBytes, session.getFileExt().toLowerCase());
        if (compressed.compressed) {
            log.info("文件压缩存储: {} ({} -> {} bytes)", mergedKey, rawBytes.length, compressed.bytes.length);
            storageService.put(mergedKey, compressed.bytes);
        }

        Map<String, Object> result = new HashMap<>();
        if (session.isIngest()) {
            // 加入知识库：登记 UPLOADED，事务提交后异步 ETL
            DocumentLifecycleService.RegisterResult r = lifecycle.register(
                    new DocumentLifecycleService.RegisterCommand(
                            mergedKey, session.getFileName(), session.getFileExt(),
                            session.getFileHash(), (long) rawBytes.length, session.getStaffId()));
            result.put("documentId", r.documentId());
            result.put("kbStatus", r.status());
            return result;
        }

        // 通用文件存储
        Docs docs = new Docs()
                .setName(mergedKey)
                .setOldName(session.getFileName())
                .setType(session.getFileExt())
                .setFileHash(session.getFileHash())
                .setSize((long) rawBytes.length / 1024)
                .setStoredSize((long) compressed.bytes.length)
                .setCompressed(compressed.compressed ? 1 : 0)
                .setStaffId(session.getStaffId());
        docsMapper.insert(docs);

        result.put("id", docs.getId());
        result.put("name", docs.getName());
        result.put("compressed", compressed.compressed);
        return result;
    }
}
