package com.qiujie.filetask.store;

import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.enums.TaskStatusEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;

/** 任务持久化与状态变更端口。 */
public interface TaskRepository {
    FileTask create(TaskTypeEnum type, TaskModuleEnum module, String fileName,
                    String sourceFilePath, String queryParams, Integer operatorId);
    FileTask getById(Long taskId);
    boolean claimRunning(Long taskId);
    void setTotalCount(Long taskId, int total);
    void increaseProgress(Long taskId, int total, int processed, int success, int fail);
    void finish(Long taskId, TaskStatusEnum status);
    void fail(Long taskId, Exception exception);
    void fail(Long taskId, String message);
    void setResultFile(Long taskId, String path);
    void deleteSourceFile(Long taskId);
    void generateErrorFile(Long taskId);
}
