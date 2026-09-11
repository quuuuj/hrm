-- V6: 合并文件中心与智能对话模块
-- 1. 文件中心合并：sys_docs 吸收 kb_document 字段，迁入存量数据并清理废弃表
ALTER TABLE `sys_docs`
  ADD COLUMN `kb_status` varchar(20) DEFAULT NULL COMMENT '知识库状态：UPLOADED/PROCESSING/READY/FAILED',
  ADD COLUMN `failure_reason` varchar(512) DEFAULT NULL COMMENT '处理失败原因',
  ADD COLUMN `preview_text` text DEFAULT NULL COMMENT '文档预览文本',
  ADD COLUMN `chunk_count` int DEFAULT 0 COMMENT '切片数量',
  ADD COLUMN `upload_time` datetime DEFAULT NULL COMMENT '上传完成时间',
  ADD COLUMN `process_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  ADD INDEX `idx_docs_kb_status` (`kb_status`);

-- 数据迁移：将 kb_document 中的文档合并入 sys_docs（注意：sys_docs.size 为 KB，kb_document.file_size 为字节）
INSERT INTO `sys_docs` (
  `name`, `type`, `old_name`, `file_hash`, `size`, `stored_size`, `compressed`,
  `staff_id`, `remark`, `create_time`, `update_time`, `is_deleted`,
  `kb_status`, `failure_reason`, `preview_text`, `chunk_count`, `upload_time`, `process_time`
)
SELECT
  d.`name`, d.`type`, d.`old_name`, d.`file_hash`,
  ROUND(d.`file_size` / 1024), d.`file_size`, 0,
  d.`staff_id`, NULL, d.`create_time`, d.`update_time`, d.`is_deleted`,
  d.`status`, d.`failure_reason`, d.`preview_text`, d.`chunk_count`, d.`upload_time`, d.`process_time`
FROM `kb_document` d;

-- 删除已被 sys_docs 吸收的知识库文档表与废弃统计表
DROP TABLE IF EXISTS `kb_document`;
DROP TABLE IF EXISTS `ast_chat_llm_usage`;

-- 2. 智能对话表重命名与结构收缩（com.qiujie.assistant -> com.qiujie.chat）
DROP TABLE IF EXISTS `ast_chat_session_context`;

RENAME TABLE `ast_chat_session` TO `chat_session`;
RENAME TABLE `ast_chat_message` TO `chat_message`;

ALTER TABLE `chat_session`
  DROP COLUMN `mode`,
  DROP COLUMN `total_tokens`;

ALTER TABLE `chat_message`
  DROP COLUMN `tool_mode`;

-- 3. 菜单名称更新：文件管理 -> 文件中心
UPDATE `per_menu` SET `name` = '文件中心' WHERE `id` = 2 AND `code` = 'docs';
