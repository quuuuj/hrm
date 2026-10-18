package com.qiujie.notification.dto;

import com.qiujie.notification.enums.NotificationCategoryEnum;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 业务侧投递到通知中心的消息。
 * <p>
 * 收件人由业务模块解析（谁该收到通知属于业务知识），通知中心只负责落库与推送。
 * </p>
 *
 * @author qiujie
 */
@Getter
public class NotificationMessage {

    private final List<Integer> staffIds;
    private final String type;
    private final NotificationCategoryEnum category;
    private final String title;
    private final String body;
    private final String bizType;
    private final Long bizId;

    private NotificationMessage(List<Integer> staffIds, String type, NotificationCategoryEnum category,
                                String title, String body, String bizType, Long bizId) {
        this.staffIds = staffIds;
        this.type = type;
        this.category = category;
        this.title = title;
        this.body = body;
        this.bizType = bizType;
        this.bizId = bizId;
    }

    /** 单个收件人 */
    public static NotificationMessage to(Integer staffId, String type, NotificationCategoryEnum category,
                                         String title, String body, String bizType, Long bizId) {
        // 允许 null，由 NotificationService.send 过滤；通知失败不应阻断业务
        return new NotificationMessage(Collections.singletonList(staffId),
                type, category, title, body, bizType, bizId);
    }

    /** 多个收件人 */
    public static NotificationMessage to(List<Integer> staffIds, String type, NotificationCategoryEnum category,
                                         String title, String body, String bizType, Long bizId) {
        return new NotificationMessage(staffIds, type, category, title, body, bizType, bizId);
    }
}
