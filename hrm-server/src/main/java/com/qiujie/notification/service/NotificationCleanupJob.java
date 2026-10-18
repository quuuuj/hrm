package com.qiujie.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 过期通知清理任务：按保留期物理删除历史通知。
 *
 * @author qiujie
 */
@Component
public class NotificationCleanupJob {

    private static final int BATCH_SIZE = 1000;

    @Value("${notification.retention-days}")
    private long retentionDays;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredNotifications() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int deleted;
        do {
            deleted = notificationService.deleteExpired(before, BATCH_SIZE);
        } while (deleted == BATCH_SIZE);
    }
}
