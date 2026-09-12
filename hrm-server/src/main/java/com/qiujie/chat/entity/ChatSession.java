package com.qiujie.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一问答会话实体 (MySQL)。
 */
@Data
@Accessors(chain = true)
@TableName("chat_session")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("title")
    private String title;

    @TableField("status")
    private String status;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
