package com.qiujie.filetask.store;

import com.qiujie.filetask.service.FileTaskService;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.enums.TaskStatusEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** 兼容现有 FileTaskService 的任务仓储适配器。 */
@Component
@Primary
public class FileTaskRepositoryAdapter implements TaskRepository {
    private final FileTaskService service;
    public FileTaskRepositoryAdapter(FileTaskService service) { this.service = service; }
    public FileTask create(TaskTypeEnum type, TaskModuleEnum module, String fileName, String source, String params, Integer operator) {
        return service.createTask(type, module, fileName, source, params, operator);
    }
    public FileTask getById(Long id) { return service.getById(id); }
    public boolean claimRunning(Long id) { return service.claimRunning(id); }
    public void setTotalCount(Long id, int total) { service.setTotalCount(id, total); }
    public void increaseProgress(Long id, int total, int processed, int success, int fail) { service.increaseProgress(id, total, processed, success, fail); }
    public void finish(Long id, TaskStatusEnum status) { service.finish(id, status); }
    public void fail(Long id, Exception exception) { service.fail(id, exception); }
    public void fail(Long id, String message) { service.fail(id, message); }
    public void setResultFile(Long id, String path) { service.setResultFile(id, path); }
    public void deleteSourceFile(Long id) { service.deleteSourceFile(id); }
    public void generateErrorFile(Long id) { service.generateErrorFile(id); }
}
