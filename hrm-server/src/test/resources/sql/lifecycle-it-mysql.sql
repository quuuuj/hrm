-- 文档生命周期集成测试：MySQL 初始化（容器默认库 hrm，脚本以 test 用户在该库内执行）
CREATE TABLE IF NOT EXISTS sys_docs (
  `id`             int             NOT NULL AUTO_INCREMENT,
  `name`           varchar(200)    NOT NULL COMMENT '存储文件名(UUID)',
  `old_name`       varchar(500)    NOT NULL COMMENT '原始文件名',
  `type`           varchar(10)     NOT NULL COMMENT '文件扩展名',
  `file_hash`      varchar(64)     NOT NULL COMMENT 'SHA-256',
  `size`           bigint          DEFAULT NULL COMMENT '文件大小(KB)',
  `stored_size`    bigint          DEFAULT NULL COMMENT '存储大小(字节)',
  `compressed`     tinyint         DEFAULT 0 COMMENT '是否压缩',
  `staff_id`       int             NOT NULL COMMENT '上传者',
  `remark`         varchar(255)    DEFAULT NULL COMMENT '备注',
  `kb_status`      varchar(20)     DEFAULT NULL COMMENT 'UPLOADED/PROCESSING/READY/FAILED',
  `failure_reason` varchar(512)    DEFAULT NULL COMMENT '失败原因',
  `preview_text`   text            DEFAULT NULL COMMENT '文档预览文本',
  `upload_time`    datetime        DEFAULT NULL COMMENT '上传完成时间',
  `process_time`   datetime        DEFAULT NULL COMMENT '处理完成时间',
  `chunk_count`    int             DEFAULT 0 COMMENT '切片数量',
  `create_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     tinyint         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_docs_kb_status` (`kb_status`),
  KEY `idx_docs_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文档管理表';

CREATE TABLE IF NOT EXISTS ingestion_jobs (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `document_id`    bigint        NOT NULL COMMENT '关联文档ID',
  `staff_id`       int           NOT NULL COMMENT '上传者',
  `job_type`       varchar(32)   NOT NULL DEFAULT 'INGEST_DOCUMENT' COMMENT '任务类型',
  `status`         varchar(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED',
  `retry_count`    int           NOT NULL DEFAULT 0 COMMENT '当前重试次数',
  `max_retries`    int           NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `worker_id`      varchar(128)  DEFAULT NULL COMMENT '执行Worker标识',
  `started_at`     datetime      DEFAULT NULL COMMENT '开始执行时间',
  `finished_at`    datetime      DEFAULT NULL COMMENT '完成时间',
  `next_retry_at`  datetime      DEFAULT NULL COMMENT '下次重试时间',
  `last_error`     text          DEFAULT NULL COMMENT '最近失败错误信息',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_job_document` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档摄入异步任务表';
