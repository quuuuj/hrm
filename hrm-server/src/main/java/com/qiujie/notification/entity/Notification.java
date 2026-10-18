package com.qiujie.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qiujie.notification.enums.NotificationCategoryEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 站内通知。
 *
 * @author qiujie
 */
@Getter
@Setter
@TableName("sys_notification")
@Schema(description = "站内通知")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "接收人员工id")
    @TableField("staff_id")
    private Integer staffId;

    @Schema(description = "通知类型，如 LEAVE_SUBMITTED")
    @TableField("type")
    private String type;

    @Schema(description = "通知类别：INFO/SUCCESS/WARNING/ERROR")
    @TableField("category")
    private NotificationCategoryEnum category;

    @Schema(description = "通知标题")
    @TableField("title")
    private String title;

    @Schema(description = "通知内容")
    @TableField("body")
    private String body;

    @Schema(description = "业务类型，如 LEAVE")
    @TableField("biz_type")
    private String bizType;

    @Schema(description = "业务主键")
    @TableField("biz_id")
    private Long bizId;

    @Schema(description = "是否已读，0未读，1已读")
    @TableField("read_flag")
    private Integer readFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "逻辑删除，0未删除，1已删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;
}
