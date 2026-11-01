package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.mapper.DocsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档物理清理（模块内部组件）：引用计数 → MinIO 物理文件 → PG 切片/向量。
 * <p>
 * 整体幂等可重跑——崩溃于中途的残留由启动恢复重调 purge 兜底。
 */
final class KnowledgePurgeHandler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgePurgeHandler.class);

    private final DocsMapper docsMapper;
    private final ChunkVectorStore chunkVectorStore;
    private final ObjectStore objectStore;

    KnowledgePurgeHandler(DocsMapper docsMapper,
                         ChunkVectorStore chunkVectorStore,
                         ObjectStore objectStore) {
        this.docsMapper = docsMapper;
        this.chunkVectorStore = chunkVectorStore;
        this.objectStore = objectStore;
    }

    /** 清理文档的物理产物；幂等。 */
    void purge(Long docsId, String storageKey) {
        // 引用计数只统计存活文档：为 0 才删物理文件（同一文件可能被其他文档共享）
        if (docsMapper.countLiveByFileName(storageKey, docsId) == 0) {
            try {
                objectStore.deleteObject(storageKey);
            } catch (Exception e) {
                log.warn("删除物理文件失败: {}", storageKey, e);
            }
        }
        chunkVectorStore.deleteChunks(docsId);
    }
}
