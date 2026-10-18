package com.qiujie.notification.dto;

import com.qiujie.notification.entity.Notification;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通知事件 DTO，通过 SSE 推送到前端。
 *
 * @author qiujie
 */
@Getter
@Setter
public class NotificationEvent {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Integer id;
    private String type;
    private String category;
    private String title;
    private String body;
    private String bizType;
    private Long bizId;
    private String time;

    public NotificationEvent() {
    }

    public static NotificationEvent from(Notification notification) {
        NotificationEvent event = new NotificationEvent();
        event.setId(notification.getId());
        event.setType(notification.getType());
        event.setCategory(notification.getCategory() == null ? null : notification.getCategory().getValue());
        event.setTitle(notification.getTitle());
        event.setBody(notification.getBody());
        event.setBizType(notification.getBizType());
        event.setBizId(notification.getBizId());
        event.setTime(LocalDateTime.now().format(TIME_FORMATTER));
        return event;
    }
}
