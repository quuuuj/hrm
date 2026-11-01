package com.qiujie.knowledge.lifecycle.port;

import com.qiujie.knowledge.entity.KnowledgeChunk;

import java.util.List;
import java.util.Set;

/**
 * PG 切片与向量存储端口（单表 kb_chunk，kb 数据源）。
 * <p>
 * 切片原文、文档名快照与向量存在同一行：一次 INSERT 即落库完整产物，检索两路都命中同一张表，
 * 删除只需 {@code WHERE docs_id = ?}——不需要 JSONB 反查、不需要跨表对账。
 * </p>
 * <p>
 * 全部写入幂等可清理——{@link #deleteChunks} 是重入与补偿的唯一清理入口。
 * 逐条 INSERT、自增 ID 回填等是适配器内部细节。
 */
public interface ChunkVectorStore {

    /**
     * 切片写入草稿。embedding 为 null 表示该切片向量化失败或未执行，
     * 行仍然落库（切片原文可查、关键词检索可用），只是不参与向量检索。
     */
    record ChunkDraft(int chunkIndex, String chunkText, int tokenCount, int charStart, int charEnd,
                      float[] embedding) {}

    /** 切片写入结果（携带 PG 生成 id）。 */
    record ChunkRecord(long id, ChunkDraft draft) {}

    /**
     * 批量写入切片（含向量）并回填 PG 自增 id。
     *
     * @param docsName 显示用文档名快照，取 sys_docs.old_name
     */
    List<ChunkRecord> storeChunks(Long docsId, String docsName, List<ChunkDraft> drafts);

    /** 幂等清理某文档全部切片与向量（不存在也成功）。 */
    void deleteChunks(Long docsId);

    /** 只读：按 chunk_index 升序返回切片（不含向量列）。 */
    List<KnowledgeChunk> listChunks(Long docsId);

    /**
     * 只读：PG 侧已建立索引的全部文档 id。
     * <p>用于启动时核对索引与 {@code sys_docs} 的一致性——文档行被外部删除后，
     * 其切片与向量会变成永不被检索、也永不被清理的孤儿数据。</p>
     */
    Set<Long> listIndexedDocsIds();
}
