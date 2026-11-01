package com.qiujie.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档摄入异步任务实体 (MySQL)
 *
 * @author quuj
 */
@Data
@Accessors(chain = true)
@TableName("kb_ingestion_task")
public class KnowledgeIngestionTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("docs_id")
    private Long docsId;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("task_type")
    private String taskType;

    /** PENDING / RUNNING / SUCCEEDED / FAILED / CANCELLED */
    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retries")
    private Integer maxRetries;

    @TableField("worker_id")
    private String workerId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("last_error")
    private String lastError;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
