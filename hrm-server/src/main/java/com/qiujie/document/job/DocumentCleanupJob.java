package com.qiujie.document.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.document.entity.Document;
import com.qiujie.document.mapper.DocumentChunkMapper;
import com.qiujie.document.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库孤儿清理，两步互为补充：
 * <ul>
 *   <li>MinIO：删除 kb/ 前缀下无 kb_document 引用的对象，按 24 小时宽限期回收</li>
 *   <li>PG：删除 kb_chunk 里 document_id 已无对应文档行的切片与向量</li>
 * </ul>
 * 只扫本模块自己的前缀——文件中心的孤儿清理（{@code DocsService.cleanOrphanDocsFiles}）
 * 已把 kb/ 列入跳过清单，两边互不越界。
 */
@Component
public class DocumentCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DocumentCleanupJob.class);

    private static final String PREFIX = "kb/";

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Scheduled(cron = "0 30 4 * * ?")
    public void cleanOrphanObjects() {
        Set<String> referencedKeys = documentMapper.selectList(
                        new QueryWrapper<Document>().select("name"))
                .stream()
                .map(Document::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000;

        for (String key : storageService.listKeys()) {
            if (!key.startsWith(PREFIX) || referencedKeys.contains(key)) {
                continue;
            }
            Date lastModified = storageService.getLastModified(key);
            if (lastModified != null && lastModified.getTime() > cutoff) {
                continue;
            }
            storageService.delete(key);
            log.info("清理知识库孤儿文件: {}", key);
        }
    }

    /**
     * 清理 PG 侧的孤儿切片与向量：document_id 在 kb_document 已查不到即孤儿——既不会被检索
     * 命中，也没有 delete 流程会再清理它。删除流程崩溃、PG 侧删除失败留下的残留都在这一步收口。
     * <p>
     * 必需顺序：先取切片 id、再取存活 id。切片总在文档行提交之后写入，倒过来先读存活 id 时，
     * 恰好在此窗口提交的文档会有切片却没进存活集合，正被误删。
     * <p>
     * 不需要宽限期：行不存在时切片必然是真孤儿。
     */
    @Scheduled(cron = "0 45 4 * * ?")
    public void cleanOrphanChunks() {
        List<Long> indexedIds = chunkMapper.selectDistinctDocumentIds();
        if (indexedIds.isEmpty()) {
            return;
        }
        Set<Long> liveIds = documentMapper.selectList(new QueryWrapper<Document>().select("id"))
                .stream()
                .map(Document::getId)
                .collect(Collectors.toCollection(HashSet::new));

        List<Long> orphanIds = indexedIds.stream()
                .filter(id -> !liveIds.contains(id))
                .toList();
        if (orphanIds.isEmpty()) {
            return;
        }
        chunkMapper.deleteByDocumentIds(orphanIds);
        log.warn("清理知识库孤儿切片: documentIds={}", orphanIds);
    }
}
