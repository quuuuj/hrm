package com.qiujie.document.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.document.entity.Document;
import com.qiujie.document.mapper.DocumentMapper;
import com.qiujie.filetask.enums.UploadScopeEnum;
import com.qiujie.filetask.spi.UploadCompletionHandler;
import com.qiujie.filetask.entity.FileUploadSession;
import com.qiujie.util.StorageCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档上传完成处理器：SHA256 秒传 + zstd 压缩 + 提交 kb_document。
 * <p>
 * 落库后由 {@link DocumentService#submit} 在事务提交后触发异步 ETL。
 * 与文件中心的处理器各自独立：两张表、两套去重、两个对象前缀。
 */
@Component
public class DocumentUploadCompletionHandler implements UploadCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadCompletionHandler.class);

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DocumentService documentService;

    @Override
    public UploadScopeEnum getScope() {
        return UploadScopeEnum.DOCUMENT;
    }

    @Override
    public String getStoragePrefix() {
        return "kb";
    }

    @Override
    public Map<String, Object> checkDedup(String fileHash, Integer staffId) {
        // 去重只查本人名下知识库文档：秒传不新建行、不新建对象，范围限定在 kb_document 内，
        // 与文件中心的同名文件互不影响
        if (staffId == null) return null;
        List<Document> existing = documentMapper.selectList(
                new QueryWrapper<Document>()
                        .eq("file_hash", fileHash)
                        .eq("staff_id", staffId)
                        .orderByAsc("id")
                        .last("LIMIT 1"));
        if (existing.isEmpty()) return null;
        Document document = existing.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("id", document.getId());
        result.put("name", document.getName());
        result.put("oldName", document.getOldName());
        result.put("status", document.getStatus());
        return result;
    }

    @Override
    public void onComplete(String mergedKey, FileUploadSession session) {
        byte[] rawBytes;
        try (java.io.InputStream in = storageService.get(mergedKey)) {
            rawBytes = in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("读取合并文件失败: " + mergedKey, e);
        }

        // 与文件中心同一套压缩规则；ETL 读取侧有 magic-byte 解压通道
        StorageCompressor.CompressionResult compressed =
                StorageCompressor.tryCompress(rawBytes, session.getFileExt().toLowerCase());
        if (compressed.compressed) {
            log.info("知识库文件压缩存储: {} ({} -> {} bytes)",
                    mergedKey, rawBytes.length, compressed.bytes.length);
            storageService.put(mergedKey, compressed.bytes);
        }

        documentService.submit(new Document()
                .setName(mergedKey)
                .setOldName(session.getFileName())
                .setType(session.getFileExt())
                .setFileHash(session.getFileHash())
                .setSize((long) rawBytes.length / 1024)
                .setStoredSize((long) compressed.bytes.length)
                .setCompressed(compressed.compressed ? 1 : 0)
                .setStaffId(session.getStaffId()));
    }
}
