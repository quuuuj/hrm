package com.qiujie.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 知识库问答引用明细实体 (MySQL kb_qa_citation)。
 * <p>
 * 一条记录对应「这次问答的模型依据里，有哪一段切片」：切片 ID、来源文档名、
 * 文本快照与各检索通道得分，用于事后回溯模型当时看到的证据。
 * 只写真正进入提示词的切片，不写检索召回但被预算截掉的部分。
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("kb_qa_citation")
public class KbQaCitation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("qa_record_id")
    private Long qaRecordId;

    @TableField("chunk_id")
    private Long chunkId;

    @TableField("document_name")
    private String documentName;

    /** 切片文本快照；超长按固定字符数截断，全文可凭 chunkId 回查 */
    @TableField("chunk_text")
    private String chunkText;

    @TableField("relevance_score")
    private Double relevanceScore;

    /** VECTOR / KEYWORD / BOTH */
    @TableField("retrieval_source")
    private String retrievalSource;

    @TableField("vector_score")
    private Double vectorScore;

    @TableField("keyword_score")
    private Double keywordScore;

    @TableField("hybrid_score")
    private Double hybridScore;

    /** 证据文本快照；与 chunk_text 语义重叠，暂不使用 */
    @TableField("snippet")
    private String snippet;
}
