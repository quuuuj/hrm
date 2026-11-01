package com.qiujie.knowledge.mapper;

import com.pgvector.PGvector;
import com.qiujie.knowledge.entity.KnowledgeChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * 文档切片数据访问 — PostgreSQL (kb 数据源)。
 * 不使用 MyBatis-Plus（避免路由到 MySQL master），直接用 JdbcTemplate。
 * 切片原文与向量同表：插入时一并写 embedding，查询默认不读向量列（体积大、列表页用不到）。
 *
 * @author quuj
 */
@Component
public class KnowledgeChunkMapper {

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

    private static final RowMapper<KnowledgeChunk> ROW_MAPPER = (rs, rowNum) -> new KnowledgeChunk()
            .setId(rs.getLong("id"))
            .setDocsId(rs.getLong("docs_id"))
            .setDocsName(rs.getString("docs_name"))
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
    public KnowledgeChunk insert(KnowledgeChunk chunk, PGvector embedding) {
        jdbc.update(
                "INSERT INTO kb_chunk (docs_id, docs_name, chunk_index, chunk_text, "
                + "token_count, chunk_summary, char_start, char_end, embedding) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                chunk.getDocsId(), chunk.getDocsName(), chunk.getChunkIndex(),
                chunk.getChunkText(), chunk.getTokenCount(), chunk.getChunkSummary(),
                chunk.getCharStart(), chunk.getCharEnd(), embedding);
        Long id = jdbc.queryForObject(
                "SELECT currval(pg_get_serial_sequence('kb_chunk', 'id'))", Long.class);
        chunk.setId(id);
        return chunk;
    }

    public List<KnowledgeChunk> selectByDocsIdOrderByChunkIndex(Long docsId) {
        return jdbc.query(
                "SELECT id, docs_id, docs_name, chunk_index, chunk_text, token_count, "
                + "chunk_summary, char_start, char_end, create_time "
                + "FROM kb_chunk WHERE docs_id = ? ORDER BY chunk_index",
                ROW_MAPPER, docsId);
    }
}
