package com.qiujie.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档切片实体 (PostgreSQL)
 *
 * @author quuj
 */
@Data
@Accessors(chain = true)
@TableName("kb_chunk")
public class KnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("docs_id")
    private Long docsId;

    /** 文档显示名快照（sys_docs.old_name），检索结果直接取用，无需回查 MySQL */
    @TableField("docs_name")
    private String docsName;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("chunk_text")
    private String chunkText;

    @TableField("token_count")
    private Integer tokenCount;

    /** 切片摘要（LLM生成，可选） */
    @TableField("chunk_summary")
    private String chunkSummary;

    /** 切片在原文档中的起始字符位置 */
    @TableField("char_start")
    private Integer charStart;

    /** 切片在原文档中的结束字符位置 */
    @TableField("char_end")
    private Integer charEnd;

    @TableField("create_time")
    private LocalDateTime createTime;
}
