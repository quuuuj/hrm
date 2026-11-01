package com.qiujie.knowledge.lifecycle.port;

import com.pgvector.PGvector;
import com.qiujie.knowledge.entity.KnowledgeChunk;
import com.qiujie.knowledge.mapper.KnowledgeChunkMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ChunkVectorStore 生产适配器：JdbcTemplate 直连 kb 数据源（PG，autocommit 不可回滚）。
 * 切片原文与向量同表（kb_chunk），一次 INSERT 落库完整产物。
 */
@Component
public class JdbcChunkVectorStore implements ChunkVectorStore {

    @Autowired
    private KnowledgeChunkMapper chunkMapper;

    @Autowired
    @Qualifier("kbDataSource")
    private DataSource kbDataSource;

    private JdbcTemplate kbJdbc;

    @PostConstruct
    public void init() {
        if (kbDataSource != null) {
            this.kbJdbc = new JdbcTemplate(kbDataSource);
        }
    }

    @Override
    public List<ChunkRecord> storeChunks(Long docsId, String docsName, List<ChunkDraft> drafts) {
        List<ChunkRecord> records = new ArrayList<>();
        for (ChunkDraft draft : drafts) {
            KnowledgeChunk entity = new KnowledgeChunk()
                    .setDocsId(docsId)
                    .setDocsName(docsName == null ? "" : docsName)
                    .setChunkIndex(draft.chunkIndex())
                    .setChunkText(draft.chunkText())
                    .setTokenCount(draft.tokenCount())
                    .setCharStart(draft.charStart())
                    .setCharEnd(draft.charEnd());
            chunkMapper.insert(entity, draft.embedding() == null ? null : new PGvector(draft.embedding()));
            records.add(new ChunkRecord(entity.getId(), draft));
        }
        return records;
    }

    @Override
    public void deleteChunks(Long docsId) {
        kbJdbc.update("DELETE FROM kb_chunk WHERE docs_id = ?", docsId);
    }

    @Override
    public List<KnowledgeChunk> listChunks(Long docsId) {
        return chunkMapper.selectByDocsIdOrderByChunkIndex(docsId);
    }

    @Override
    public Set<Long> listIndexedDocsIds() {
        return new LinkedHashSet<>(kbJdbc.queryForList(
                "SELECT DISTINCT docs_id FROM kb_chunk", Long.class));
    }
}
