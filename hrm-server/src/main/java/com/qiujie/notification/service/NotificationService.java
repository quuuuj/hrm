package com.qiujie.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.common.sse.SseService;
import com.qiujie.notification.dto.NotificationEvent;
import com.qiujie.notification.dto.NotificationMessage;
import com.qiujie.notification.entity.Notification;
import com.qiujie.notification.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 通知中心：站内通知的落库、SSE 推送与已读状态维护。
 * <p>
 * 业务模块只负责构造 {@link NotificationMessage}（发生什么、通知谁），
 * 投递方式与持久化由本服务统一处理。推送失败仅告警，不影响主流程。
 * </p>
 *
 * @author qiujie
 */
@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private SseService sseService;

    /**
     * 发送站内通知：先落库保证离线可见，再通过 SSE 推送在线用户。
     * 任何异常只告警，不阻断调用方业务。
     */
    public void send(NotificationMessage message) {
        if (message == null || message.getStaffIds() == null || message.getStaffIds().isEmpty()) {
            return;
        }
        List<Integer> staffIds = message.getStaffIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (staffIds.isEmpty()) {
            return;
        }

        try {
            for (Integer staffId : staffIds) {
                Notification notification = new Notification();
                notification.setStaffId(staffId);
                notification.setType(message.getType());
                notification.setTitle(message.getTitle());
                notification.setBody(message.getBody());
                notification.setBizType(message.getBizType());
                notification.setBizId(message.getBizId());
                notification.setReadFlag(0);
                this.save(notification);

                sseService.emit(staffId, "notification",
                        new NotificationEvent(notification.getId(), notification.getType(),
                                notification.getTitle(), notification.getBody()));
            }
        } catch (Exception e) {
            log.warn("Failed to send notification: type={}, bizType={}, bizId={}",
                    message.getType(), message.getBizType(), message.getBizId(), e);
        }
    }

    /** 当前用户的通知分页，按创建时间倒序 */
    public IPage<Notification> pageByStaffId(Integer staffId, long current, long size) {
        return this.page(new Page<>(current, size), new LambdaQueryWrapper<Notification>()
                .eq(Notification::getStaffId, staffId)
                .orderByDesc(Notification::getCreateTime)
                .orderByDesc(Notification::getId));
    }

    /** 当前用户未读数量 */
    public long countUnread(Integer staffId) {
        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getStaffId, staffId)
                .eq(Notification::getReadFlag, 0));
    }

    /** 标记单条通知已读（只能操作自己的通知） */
    public boolean markRead(Integer staffId, Integer notificationId) {
        return this.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getStaffId, staffId)
                .set(Notification::getReadFlag, 1));
    }

    /** 标记当前用户全部通知已读 */
    public boolean markAllRead(Integer staffId) {
        return this.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getStaffId, staffId)
                .eq(Notification::getReadFlag, 0)
                .set(Notification::getReadFlag, 1));
    }
}
