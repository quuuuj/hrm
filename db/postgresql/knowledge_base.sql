-- ============================================================
-- HRM 知识库模块 - PostgreSQL 向量存储
-- 数据库: hrm_kb (kb 数据源)
-- ============================================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 文档切片表：切片原文 + 元数据 + 向量同表存储
-- 向量维度: 1024 = qwen3.7-text-embedding
-- embedding 可空：向量化失败或尚未执行时为 NULL，检索侧以 IS NOT NULL 过滤
CREATE TABLE IF NOT EXISTS kb_chunk (
    id              BIGSERIAL PRIMARY KEY,
    docs_id     BIGINT       NOT NULL,
    docs_name   VARCHAR(255) NOT NULL DEFAULT '',
    chunk_index     INT          NOT NULL,
    chunk_text      TEXT         NOT NULL,
    token_count     INT          DEFAULT 0,
    chunk_summary   TEXT,
    char_start      INT          DEFAULT 0,
    char_end        INT          DEFAULT 0,
    embedding       vector(1024),
    create_time     TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (docs_id, chunk_index)
);
CREATE INDEX IF NOT EXISTS idx_chunk_docs ON kb_chunk(docs_id);

-- 向量近邻索引（HNSW + 余弦距离）；部分索引跳过未向量化的行
CREATE INDEX IF NOT EXISTS idx_chunk_embedding
    ON kb_chunk USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- 列注释
COMMENT ON COLUMN kb_chunk.id IS '主键';
COMMENT ON COLUMN kb_chunk.docs_id IS '所属文档ID';
COMMENT ON COLUMN kb_chunk.docs_name IS '文档显示名快照（sys_docs.old_name）';
COMMENT ON COLUMN kb_chunk.chunk_index IS '切片序号';
COMMENT ON COLUMN kb_chunk.chunk_text IS '切片文本';
COMMENT ON COLUMN kb_chunk.token_count IS 'Token数量';
COMMENT ON COLUMN kb_chunk.chunk_summary IS '切片摘要';
COMMENT ON COLUMN kb_chunk.char_start IS '起始字符位置';
COMMENT ON COLUMN kb_chunk.char_end IS '结束字符位置';
COMMENT ON COLUMN kb_chunk.embedding IS '切片向量（1024维）';
COMMENT ON COLUMN kb_chunk.create_time IS '创建时间';
