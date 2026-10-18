package com.qiujie.filetask.notify;

import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.enums.TaskStatusEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;
import com.qiujie.filetask.mapper.FileTaskMapper;
import com.qiujie.notification.constant.NotificationBizType;
import com.qiujie.notification.dto.NotificationMessage;
import com.qiujie.notification.enums.NotificationCategoryEnum;
import com.qiujie.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 文件任务终态通知——把任务结果翻译为通用通知消息。
 * <p>
 * 由 {@code FileTaskEngine} 在任务终态后调用。通知失败只告警，不影响任务本身。
 * </p>
 *
 * @author qiujie
 */
@Component
public class FileTaskNotifier {

    private static final Logger log = LoggerFactory.getLogger(FileTaskNotifier.class);

    private static final String TYPE_SUCCESS = "FILE_TASK_SUCCESS";
    private static final String TYPE_PARTIAL_SUCCESS = "FILE_TASK_PARTIAL_SUCCESS";
    private static final String TYPE_FAILED = "FILE_TASK_FAILED";

    @Autowired
    private FileTaskMapper fileTaskMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * 任务到达终态后发送站内通知。非终态、任务不存在或没有操作者时静默返回。
     *
     * @param taskId 文件任务 id
     */
    public void onTaskTerminal(Long taskId) {
        try {
            FileTask task = fileTaskMapper.selectById(taskId);
            if (task == null || task.getOperatorId() == null || !isTerminal(task.getStatus())) {
                return;
            }

            NotificationMessage message = buildMessage(task);
            if (message != null) {
                notificationService.sendIfAbsent(message);
            }
        } catch (Exception e) {
            log.warn("Failed to notify file task terminal state: taskId={}", taskId, e);
        }
    }

    private NotificationMessage buildMessage(FileTask task) {
        if (task.getTaskType() == TaskTypeEnum.IMPORT) {
            return buildImportMessage(task);
        }
        if (task.getTaskType() == TaskTypeEnum.EXPORT) {
            return buildExportMessage(task);
        }
        return null;
    }

    private NotificationMessage buildImportMessage(FileTask task) {
        String fileName = fileName(task);
        if (task.getStatus() == TaskStatusEnum.SUCCESS) {
            return message(task, TYPE_SUCCESS, NotificationCategoryEnum.SUCCESS, "导入成功",
                    fileName + " 共 " + count(task.getTotalCount()) + " 条，全部导入成功");
        }
        if (task.getStatus() == TaskStatusEnum.PARTIAL_SUCCESS) {
            return message(task, TYPE_PARTIAL_SUCCESS, NotificationCategoryEnum.WARNING, "导入部分成功",
                    fileName + " 成功 " + count(task.getSuccessCount()) + " 条，失败 "
                            + count(task.getFailCount()) + " 条，可在任务列表下载错误文件");
        }
        return message(task, TYPE_FAILED, NotificationCategoryEnum.ERROR, "导入失败",
                fileName + " 导入失败：" + failReason(task));
    }

    private NotificationMessage buildExportMessage(FileTask task) {
        String fileName = fileName(task);
        if (task.getStatus() == TaskStatusEnum.SUCCESS) {
            return message(task, TYPE_SUCCESS, NotificationCategoryEnum.SUCCESS, "导出成功",
                    fileName + " 已生成，可在任务列表下载");
        }
        return message(task, TYPE_FAILED, NotificationCategoryEnum.ERROR, "导出失败",
                fileName + " 导出失败：" + failReason(task));
    }

    private NotificationMessage message(FileTask task, String type, NotificationCategoryEnum category,
                                        String title, String body) {
        return NotificationMessage.to(task.getOperatorId(), type, category, title, body,
                NotificationBizType.FILE_TASK, task.getId());
    }

    private boolean isTerminal(TaskStatusEnum status) {
        return status == TaskStatusEnum.SUCCESS
                || status == TaskStatusEnum.PARTIAL_SUCCESS
                || status == TaskStatusEnum.FAILED;
    }

    private String fileName(FileTask task) {
        return task.getFileName() == null ? "文件" : task.getFileName();
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }

    private String failReason(FileTask task) {
        String reason = task.getFailReason();
        return reason == null || reason.isBlank() ? "未知原因" : reason;
    }
}
