package com.qiujie.document.mapper;

import com.pgvector.PGvector;
import com.qiujie.document.entity.DocumentChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 文档切片数据访问 — PostgreSQL (kb 数据源)。
 * 不使用 MyBatis-Plus（避免路由到 MySQL master），直接用 JdbcTemplate。
 * 切片原文与向量同表：插入时一并写 embedding，查询默认不读向量列（体积大、列表页用不到）。
 *
 * @author quuj
 */
@Component
public class DocumentChunkMapper {

    @Autowired
    @Qualifier("kbDataSource")
    private DataSource kbDataSource;

    private JdbcTemplate jdbc;

    @PostConstruct
    public void init() {
        if (kbDataSource != null) {
            this.jdbc = new JdbcTemplate(kbDataSource);
        }
    }

    private static final RowMapper<DocumentChunk> ROW_MAPPER = (rs, rowNum) -> new DocumentChunk()
            .setId(rs.getLong("id"))
            .setDocumentId(rs.getLong("document_id"))
            .setDocumentName(rs.getString("document_name"))
            .setChunkIndex(rs.getInt("chunk_index"))
            .setChunkText(rs.getString("chunk_text"))
            .setTokenCount(rs.getInt("token_count"))
            .setChunkSummary(rs.getString("chunk_summary"))
            .setCharStart(rs.getInt("char_start"))
            .setCharEnd(rs.getInt("char_end"));

    /**
     * 插入切片并回填自增 id。
     *
     * @param embedding 向量；null 表示向量化失败或未执行，写入 NULL（不参与向量检索）
     */
    public DocumentChunk insert(DocumentChunk chunk, float[] embedding) {
        jdbc.update(
                "INSERT INTO kb_chunk (document_id, document_name, chunk_index, chunk_text, "
                + "token_count, chunk_summary, char_start, char_end, embedding) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                chunk.getDocumentId(), chunk.getDocumentName(), chunk.getChunkIndex(),
                chunk.getChunkText(), chunk.getTokenCount(), chunk.getChunkSummary(),
                chunk.getCharStart(), chunk.getCharEnd(),
                embedding == null ? null : new PGvector(embedding));
        Long id = jdbc.queryForObject(
                "SELECT currval(pg_get_serial_sequence('kb_chunk', 'id'))", Long.class);
        chunk.setId(id);
        return chunk;
    }

    /** 已落过切片的文档 id（去重），供孤儿清理与 kb_document 比对。 */
    public List<Long> selectDistinctDocumentIds() {
        return jdbc.queryForList("SELECT DISTINCT document_id FROM kb_chunk", Long.class);
    }

    public List<DocumentChunk> selectByDocumentIdOrderByChunkIndex(Long documentId) {
        return jdbc.query(
                "SELECT id, document_id, document_name, chunk_index, chunk_text, token_count, "
                + "chunk_summary, char_start, char_end, create_time "
                + "FROM kb_chunk WHERE document_id = ? ORDER BY chunk_index",
                ROW_MAPPER, documentId);
    }

    /** 幂等清理某文档全部切片与向量（不存在也成功）。 */
    public void deleteByDocumentId(Long documentId) {
        jdbc.update("DELETE FROM kb_chunk WHERE document_id = ?", documentId);
    }

    /**
     * 批量清理多个文档的全部切片与向量：一条 DELETE 走 idx_chunk_document。
     * 文档数量由页面选择决定（最多一页），占位符数量随之变化，无需额外约束。
     */
    public void deleteByDocumentIds(Collection<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(documentIds.size(), "?"));
        jdbc.update("DELETE FROM kb_chunk WHERE document_id IN (" + placeholders + ")",
                documentIds.toArray());
    }
}
