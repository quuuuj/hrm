package com.qiujie.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库问答记录实体 (MySQL kb_qa_record)。
 * <p>
 * 每次问答一行，是「谁问了什么、答了什么、证据够不够」的汇总。
 * 引用明细见 {@link KbQaCitation}（1:N）。
 * </p>
 * <p>
 * token / 延迟 / 拒答原因等可观测字段已建表但暂未写入，留待后续补齐；
 * 非知识分支（无关/闲聊）没有证据等级，{@code evidenceLevel} 与 {@code answer} 因此可空。
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("kb_qa_record")
public class KbQaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("question")
    private String question;

    @TableField("answer")
    private String answer;

    @TableField("staff_id")
    private Integer staffId;

    /** NONE / WEAK / PARTIAL / SUFFICIENT；非知识分支为 null */
    @TableField("evidence_level")
    private String evidenceLevel;

    @TableField("answered")
    private Boolean answered;

    @TableField("reason_code")
    private String reasonCode;

    @TableField("reason_message")
    private String reasonMessage;

    @TableField("citation_count")
    private Integer citationCount;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("is_estimated")
    private Boolean isEstimated;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("model_name")
    private String modelName;

    @TableField("endpoint")
    private String endpoint;

    @TableField("success")
    private Boolean success;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private LocalDateTime createTime;
}
