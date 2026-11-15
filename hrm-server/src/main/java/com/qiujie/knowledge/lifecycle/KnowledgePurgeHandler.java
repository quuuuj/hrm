package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文档物理清理（模块内部组件）：MinIO 物理文件 → PG 切片/向量。
 * <p>
 * 行已物理删除，物理文件不再有任何存活引用（{@code sys_docs.name} 唯一），直接删。
 * 对象删除失败只记 warn：对象键已随行消失，物理文件由孤儿清理任务（24 小时后）兜底回收，
 * 清理任务本身不中断，PG 切片仍会被删掉。
 */
final class KnowledgePurgeHandler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgePurgeHandler.class);

    private final ChunkVectorStore chunkVectorStore;
    private final ObjectStore objectStore;

    KnowledgePurgeHandler(ChunkVectorStore chunkVectorStore, ObjectStore objectStore) {
        this.chunkVectorStore = chunkVectorStore;
        this.objectStore = objectStore;
    }

    /** 清理文档的物理产物；幂等（S3 removeObject 对不存在对象不报错）。 */
    void purge(Long docsId, String storageKey) {
        try {
            objectStore.deleteObject(storageKey);
        } catch (Exception e) {
            log.warn("删除物理文件失败（对象将由孤儿清理兜底）: {}", storageKey, e);
        }
        chunkVectorStore.deleteChunks(docsId);
    }
}
