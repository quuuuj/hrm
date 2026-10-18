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
import com.qiujie.notification.enums.NotificationCategoryEnum;
import com.qiujie.notification.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int BODY_MAX_LENGTH = 500;
    private static final int DELETE_BATCH_SIZE = 500;

    @Autowired
    private SseService sseService;

    /**
     * 发送站内通知：先落库保证离线可见，再通过 SSE 推送在线用户。
     * 任何异常只告警，不阻断调用方业务；单个收件人失败不影响其余收件人。
     */
    public void send(NotificationMessage message) {
        dispatch(message, false);
    }

    /**
     * 幂等发送：同一收件人对同一个 (bizType, bizId, type) 只落库一次。
     * 用于文件任务等只会到达一次终态的业务通知。
     */
    public void sendIfAbsent(NotificationMessage message) {
        dispatch(message, true);
    }

    private void dispatch(NotificationMessage message, boolean skipIfExists) {
        for (Integer staffId : distinctRecipients(message)) {
            sendToOne(staffId, message, skipIfExists);
        }
    }

    private void sendToOne(Integer staffId, NotificationMessage message, boolean skipIfExists) {
        try {
            if (skipIfExists && exists(staffId, message.getBizType(), message.getBizId(), message.getType())) {
                return;
            }

            Notification notification = new Notification();
            notification.setStaffId(staffId);
            notification.setType(message.getType());
            notification.setCategory(message.getCategory() == null
                    ? NotificationCategoryEnum.INFO : message.getCategory());
            notification.setTitle(truncate(message.getTitle(), TITLE_MAX_LENGTH));
            notification.setBody(truncate(message.getBody(), BODY_MAX_LENGTH));
            notification.setBizType(message.getBizType());
            notification.setBizId(message.getBizId());
            notification.setReadFlag(0);
            this.save(notification);

            pushAfterCommit(staffId, notification);
        } catch (Exception e) {
            log.warn("Failed to send notification: staffId={}, type={}, bizType={}, bizId={}",
                    staffId, message.getType(), message.getBizType(), message.getBizId(), e);
        }
    }

    /**
     * 有事务时等提交后再推送，避免回滚产生幻影通知；无事务时立即推送。
     */
    private void pushAfterCommit(Integer staffId, Notification notification) {
        Runnable push = () -> {
            try {
                sseService.emit(staffId, "notification", NotificationEvent.from(notification));
            } catch (Exception e) {
                log.warn("Failed to push notification: id={}, staffId={}", notification.getId(), staffId, e);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    private List<Integer> distinctRecipients(NotificationMessage message) {
        if (message == null || message.getStaffIds() == null || message.getStaffIds().isEmpty()) {
            return List.of();
        }
        return message.getStaffIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 判断同一收件人是否已存在相同业务终态的通知。
     */
    public boolean exists(Integer staffId, String bizType, Long bizId, String type) {
        if (staffId == null || bizType == null || bizId == null || type == null) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getStaffId, staffId)
                .eq(Notification::getBizType, bizType)
                .eq(Notification::getBizId, bizId)
                .eq(Notification::getType, type)) > 0;
    }

    /**
     * 按业务类型和业务主键批量物理删除通知，供文件任务清理使用。
     *
     * @return 删除条数
     */
    public int deleteByBizTypeAndIds(String bizType, Collection<Long> bizIds) {
        if (bizType == null || bizIds == null || bizIds.isEmpty()) {
            return 0;
        }
        List<Long> ids = bizIds.stream().filter(Objects::nonNull).distinct().toList();
        int deleted = 0;
        for (int from = 0; from < ids.size(); from += DELETE_BATCH_SIZE) {
            int to = Math.min(from + DELETE_BATCH_SIZE, ids.size());
            deleted += baseMapper.deleteByBizTypeAndIds(bizType, new ArrayList<>(ids.subList(from, to)));
        }
        return deleted;
    }

    /**
     * 物理删除创建时间早于 before 的通知，单次最多 limit 条。
     */
    public int deleteExpired(LocalDateTime before, int limit) {
        if (before == null || limit <= 0) {
            return 0;
        }
        return baseMapper.deleteExpired(before, limit);
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
