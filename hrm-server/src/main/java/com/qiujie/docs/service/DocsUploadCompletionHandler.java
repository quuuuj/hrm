package com.qiujie.docs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.docs.entity.Docs;
import com.qiujie.docs.mapper.DocsMapper;
import com.qiujie.filetask.enums.UploadScopeEnum;
import com.qiujie.filetask.spi.UploadCompletionHandler;
import com.qiujie.filetask.entity.FileUploadSession;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.util.StorageCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件中心上传完成处理器：SHA256 秒传 + zstd 压缩 + 写入 sys_docs。
 * <p>
 * 只处理普通附件。知识库文档走 {@code DocumentUploadCompletionHandler}
 * （scope=DOCUMENT，对象前缀 kb/）。
 */
@Component
public class DocsUploadCompletionHandler implements UploadCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(DocsUploadCompletionHandler.class);

    @Autowired
    private DocsMapper docsMapper;

    @Autowired
    private MinioStorageService storageService;

    @Override
    public UploadScopeEnum getScope() {
        return UploadScopeEnum.DOCS;
    }

    @Override
    public String getStoragePrefix() {
        return "docs";
    }

    @Override
    public Map<String, Object> checkDedup(String fileHash, Integer staffId) {
        // 去重只查本人名下文档：秒传不新建 sys_docs 行、不新建对象，跨员工命中会让
        // 秒传方拿到他人文档的 id 与对象键，对方一旦删除，对象随之被物理清理
        if (staffId == null) return null;
        // 行物理删除，库里只剩存活文档：命中即代表该对象仍被本人引用
        List<Docs> existing = docsMapper.selectList(
                new QueryWrapper<Docs>()
                        .eq("file_hash", fileHash)
                        .eq("staff_id", staffId)
                        // file_hash 非唯一索引，同一人多行时取最早的一条，行为可预期
                        .orderByAsc("id")
                        .last("LIMIT 1"));
        if (existing.isEmpty()) return null;
        Docs doc = existing.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("id", doc.getId());
        result.put("name", doc.getName());
        result.put("oldName", doc.getOldName());
        return result;
    }

    @Override
    public void onComplete(String mergedKey, FileUploadSession session) {
        // 读取合并后的文件内容用于压缩
        byte[] rawBytes;
        try (java.io.InputStream in = storageService.get(mergedKey)) {
            rawBytes = in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("读取合并文件失败: " + mergedKey, e);
        }

        // zstd 压缩（文本类小文件统一压缩存储，下载时按 magic-byte 解压）
        StorageCompressor.CompressionResult compressed =
                StorageCompressor.tryCompress(rawBytes, session.getFileExt().toLowerCase());
        if (compressed.compressed) {
            log.info("文件压缩存储: {} ({} -> {} bytes)", mergedKey, rawBytes.length, compressed.bytes.length);
            storageService.put(mergedKey, compressed.bytes);
        }

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
    }
}
