package com.qiujie.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一问答消息实体 (MySQL)。
 * <p>tool_mode/structured_payload 列随工具体系删除（RFC #69）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    /** USER / ASSISTANT */
    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField("create_time")
    private LocalDateTime createTime;
}
