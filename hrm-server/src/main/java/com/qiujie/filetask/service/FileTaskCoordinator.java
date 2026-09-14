package com.qiujie.filetask.service;

import com.qiujie.filetask.engine.FileTaskEngine;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;
import com.qiujie.filetask.spi.AsyncFileTasks;
import com.qiujie.filetask.spi.ExportProcessor;
import com.qiujie.filetask.spi.ImportProcessor;
import com.qiujie.filetask.spi.ImportReader;
import com.qiujie.filetask.spi.TaskSnapshot;
import com.qiujie.filetask.store.TaskRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 文件任务生命周期门面。
 * <p>
 * 业务模块只描述导入/导出的业务处理器，任务创建、异步提交和执行入口由本类统一拥有。
 * 查询、鉴权与下载委托给 {@link FileTaskQueryService}，生命周期持久化委托给 {@link TaskRepository}。
 * </p>
 */
@Service
public class FileTaskCoordinator implements AsyncFileTasks {

    private final TaskRepository taskRepository;
    private final FileTaskQueryService queryService;
    private final FileTaskEngine fileTaskEngine;
    private final ThreadPoolTaskExecutor fileTaskExecutor;

    public FileTaskCoordinator(TaskRepository taskRepository,
                               FileTaskQueryService queryService,
                               FileTaskEngine fileTaskEngine,
                               @Qualifier("fileTaskExecutor") ThreadPoolTaskExecutor fileTaskExecutor) {
        this.taskRepository = taskRepository;
        this.queryService = queryService;
        this.fileTaskEngine = fileTaskEngine;
        this.fileTaskExecutor = fileTaskExecutor;
    }

    @Override
    public <T> TaskSnapshot submitImport(AsyncFileTasks.ImportRequest<T> request) {
        TaskSubmission result = request.reader() == null
                ? submitImport(new ImportCommand(request.module(), request.fileName(), request.sourceFilePath(),
                        request.queryParams(), request.operatorId()), request.processor())
                : submitImport(new ImportCommand(request.module(), request.fileName(), request.sourceFilePath(),
                        request.queryParams(), request.operatorId()), request.processor(), request.reader());
        return new TaskSnapshot(result.taskId(), result.snapshot());
    }

    @Override
    public <T> TaskSnapshot submitExport(AsyncFileTasks.ExportRequest<T> request) {
        TaskSubmission result = submitExport(new ExportCommand(request.module(), request.fileName(),
                request.queryParams(), request.operatorId()), request.processor());
        return new TaskSnapshot(result.taskId(), result.snapshot());
    }

    /** 导入任务命令。 */
    public record ImportCommand(TaskModuleEnum module,
                                String fileName,
                                String sourceFilePath,
                                String queryParams,
                                Integer operatorId) {
    }

    /** 导出任务命令。 */
    public record ExportCommand(TaskModuleEnum module,
                                String fileName,
                                String queryParams,
                                Integer operatorId) {
    }

    /** 提交结果，保留现有 FileTask 作为 API 兼容的初始快照。 */
    public record TaskSubmission(Long taskId, FileTask snapshot) {
    }

    /** 使用默认 Excel reader 提交导入任务。 */
    public <T> TaskSubmission submitImport(ImportCommand command, ImportProcessor<T> processor) {
        return submitImport(command, processor, null);
    }

    /** 使用业务指定 reader 提交导入任务。 */
    public <T> TaskSubmission submitImport(ImportCommand command,
                                           ImportProcessor<T> processor,
                                           ImportReader<T> reader) {
        FileTask task = taskRepository.create(
                TaskTypeEnum.IMPORT,
                command.module(),
                command.fileName(),
                command.sourceFilePath(),
                command.queryParams(),
                command.operatorId());
        fileTaskExecutor.execute(() -> {
            if (reader == null) {
                fileTaskEngine.runImport(task.getId(), processor);
            } else {
                fileTaskEngine.runImport(task.getId(), processor, reader);
            }
        });
        return new TaskSubmission(task.getId(), task);
    }

    /** 提交导出任务。 */
    public <T> TaskSubmission submitExport(ExportCommand command, ExportProcessor<T> processor) {
        FileTask task = taskRepository.create(
                TaskTypeEnum.EXPORT,
                command.module(),
                command.fileName(),
                null,
                command.queryParams(),
                command.operatorId());
        fileTaskExecutor.execute(() -> fileTaskEngine.runExport(
                task.getId(), processor, command.queryParams(), command.fileName()));
        return new TaskSubmission(task.getId(), task);
    }

    /** 订阅当前操作者的任务状态事件。 */
    public SseEmitter subscribe() {
        return queryService.subscribeSse();
    }

    /** 查询当前操作者可访问的任务列表。 */
    public com.qiujie.common.dto.ResponseDTO list(Integer current, Integer size,
                                                  String taskType, String module) {
        return queryService.list(current, size, taskType, module);
    }

    /** 查询当前操作者可访问的任务详情。 */
    public com.qiujie.common.dto.ResponseDTO inspect(Long taskId) {
        return queryService.inspect(taskId);
    }

    /** 查询当前操作者可访问的导入错误。 */
    public com.qiujie.common.dto.ResponseDTO queryErrors(Long taskId, Integer current, Integer size) {
        return queryService.queryErrors(taskId, current, size);
    }

    /** 将任务文件下载到 HTTP 响应，隐藏本地文件和对象存储差异。 */
    public void download(Long taskId, String fileType, HttpServletResponse response) throws IOException {
        queryService.download(taskId, fileType, response);
    }
}
