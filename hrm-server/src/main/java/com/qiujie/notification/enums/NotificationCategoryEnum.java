package com.qiujie.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类别：决定前端展示语义，不承载业务含义。
 *
 * @author qiujie
 */
@Getter
@AllArgsConstructor
public enum NotificationCategoryEnum {

    /** 一般信息、待办提醒 */
    INFO("INFO"),

    /** 业务成功终态 */
    SUCCESS("SUCCESS"),

    /** 部分成功、需要关注 */
    WARNING("WARNING"),

    /** 业务失败 */
    ERROR("ERROR");

    @EnumValue
    private final String value;
}
