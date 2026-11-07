package com.qiujie.filetask.store;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.sse.SseService;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.enums.TaskStatusEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;
import com.qiujie.filetask.mapper.FileTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 文件任务的持久化与状态流转。
 * <p>
 * 负责任务创建、CAS 状态认领、进度累加、终态结算与状态变更后的 SSE 推送。
 * 错误文件的生成归引擎，源文件清理归调用方。
 * </p>
 */
@Component
public class FileTaskStore {

    @Autowired
    private FileTaskMapper fileTaskMapper;
    @Autowired
    private MinioArtifactStore artifactStore;
    @Autowired
    private SseService sseService;

    public FileTask create(TaskTypeEnum taskType, TaskModuleEnum module, String fileName,
                           String sourceFilePath, String queryParams, Integer operatorId) {
        FileTask fileTask = new FileTask()
                .setTaskType(taskType)
                .setModule(module)
                .setStatus(TaskStatusEnum.PENDING)
                .setFileName(fileName)
                .setSourceFilePath(sourceFilePath)
                .setQueryParams(queryParams)
                .setTotalCount(0)
                .setProcessedCount(0)
                .setSuccessCount(0)
                .setFailCount(0)
                .setOperatorId(operatorId);
        fileTaskMapper.insert(fileTask);
        pushTaskEvent(fileTask.getId());
        return fileTask;
    }

    public FileTask getById(Long id) {
        return fileTaskMapper.selectById(id);
    }

    public boolean claimRunning(Long id) {
        boolean claimed = fileTaskMapper.claimRunning(id) == 1;
        if (claimed) {
            pushTaskEvent(id);
        }
        return claimed;
    }

    public void setTotalCount(Long id, int total) {
        fileTaskMapper.updateById(new FileTask().setId(id).setTotalCount(total));
    }

    public void increaseProgress(Long id, int total, int processed, int success, int fail) {
        fileTaskMapper.increaseProgress(id, total, processed, success, fail);
        pushTaskEvent(id);
    }

    public void finish(Long id, TaskStatusEnum status) {
        fileTaskMapper.updateById(new FileTask()
                .setId(id)
                .setStatus(status)
                .setFinishTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    public void fail(Long id, Exception e) {
        fail(id, e == null ? null : e.getMessage());
    }

    public void fail(Long id, String message) {
        if (message != null && message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        fileTaskMapper.updateById(new FileTask()
                .setId(id)
                .setStatus(TaskStatusEnum.FAILED)
                .setFailReason(message)
                .setFinishTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    public void setResultFile(Long id, String resultFilePath) {
        fileTaskMapper.updateById(new FileTask().setId(id).setResultFilePath(resultFilePath));
    }

    public void setErrorFile(Long id, String errorFilePath) {
        fileTaskMapper.updateById(new FileTask().setId(id).setErrorFilePath(errorFilePath));
    }

    /** 删除任务源文件；源文件 key 为空时忽略。 */
    public void deleteSourceFile(Long taskId) {
        FileTask task = getById(taskId);
        if (task != null && task.getSourceFilePath() != null) {
            artifactStore.delete(task.getSourceFilePath());
        }
    }

    private void pushTaskEvent(Long id) {
        FileTask task = getById(id);
        if (task != null && task.getOperatorId() != null) {
            sseService.emit(task.getOperatorId(), "task-update", task);
        }
    }
}
