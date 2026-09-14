package com.qiujie.filetask.store;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.sse.SseService;
import com.qiujie.filetask.dto.FileTaskErrorExportRow;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.enums.TaskStatusEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;
import com.qiujie.filetask.mapper.FileTaskMapper;
import com.qiujie.filetask.service.FileTaskErrorService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus 的 TaskRepository 生产实现。
 * 负责任务持久化、CAS 状态认领、进度流转、错误文件导出及结果结算。
 */
@Repository
@Primary
public class MyBatisTaskRepository implements TaskRepository {

    private static final int ERROR_EXPORT_PAGE_SIZE = 1000;

    private final FileTaskMapper fileTaskMapper;
    private final FileTaskErrorService fileTaskErrorService;
    private final ArtifactStore artifactStore;
    private final SseService sseService;

    public MyBatisTaskRepository(FileTaskMapper fileTaskMapper,
                                 FileTaskErrorService fileTaskErrorService,
                                 ArtifactStore artifactStore,
                                 SseService sseService) {
        this.fileTaskMapper = fileTaskMapper;
        this.fileTaskErrorService = fileTaskErrorService;
        this.artifactStore = artifactStore;
        this.sseService = sseService;
    }

    @Override
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

    @Override
    public FileTask getById(Long id) {
        return fileTaskMapper.selectById(id);
    }

    @Override
    public boolean claimRunning(Long id) {
        boolean claimed = fileTaskMapper.claimRunning(id) == 1;
        if (claimed) {
            pushTaskEvent(id);
        }
        return claimed;
    }

    public void markRunning(Long id) {
        fileTaskMapper.updateById(new FileTask()
                .setId(id)
                .setStatus(TaskStatusEnum.RUNNING)
                .setStartTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    @Override
    public void setTotalCount(Long id, int total) {
        fileTaskMapper.updateById(new FileTask().setId(id).setTotalCount(total));
    }

    @Override
    public void increaseProgress(Long id, int total, int processed, int success, int fail) {
        fileTaskMapper.increaseProgress(id, total, processed, success, fail);
        pushTaskEvent(id);
    }

    @Override
    public void finish(Long id, TaskStatusEnum status) {
        fileTaskMapper.updateById(new FileTask()
                .setId(id)
                .setStatus(status)
                .setFinishTime(Timestamp.valueOf(LocalDateTime.now())));
        pushTaskEvent(id);
    }

    @Override
    public void fail(Long id, Exception e) {
        fail(id, e == null ? null : e.getMessage());
    }

    @Override
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

    @Override
    public void setResultFile(Long id, String resultFilePath) {
        fileTaskMapper.updateById(new FileTask().setId(id).setResultFilePath(resultFilePath));
    }

    public void setErrorFile(Long id, String errorFilePath) {
        fileTaskMapper.updateById(new FileTask().setId(id).setErrorFilePath(errorFilePath));
    }

    @Override
    public void deleteSourceFile(Long taskId) {
        FileTask task = getById(taskId);
        if (task != null && task.getSourceFilePath() != null) {
            artifactStore.delete(task.getSourceFilePath());
        }
    }

    @Override
    public void generateErrorFile(Long taskId) {
        File errorFile = artifactStore.createTaskFile("task-error", "import-errors.xlsx");
        ExcelWriter excelWriter = EasyExcel.write(errorFile, FileTaskErrorExportRow.class).build();
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet("errors").build();
            long lastId = 0;
            while (true) {
                QueryWrapper<FileTaskError> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("task_id", taskId).gt("id", lastId)
                        .orderByAsc("id").last("limit " + ERROR_EXPORT_PAGE_SIZE);
                List<FileTaskError> errors = fileTaskErrorService.list(queryWrapper);
                if (errors.isEmpty()) {
                    break;
                }
                List<FileTaskErrorExportRow> rows = errors.stream()
                        .map(error -> new FileTaskErrorExportRow()
                                .setRowNum(error.getRowNum())
                                .setRawData(error.getRawData())
                                .setErrorMessage(error.getErrorMessage()))
                        .collect(Collectors.toList());
                excelWriter.write(rows, writeSheet);
                lastId = errors.get(errors.size() - 1).getId();
            }
        } finally {
            excelWriter.finish();
        }
        String key = artifactStore.upload(errorFile, "task-error");
        setErrorFile(taskId, key);
    }

    private void pushTaskEvent(Long id) {
        FileTask task = getById(id);
        if (task != null && task.getOperatorId() != null) {
            sseService.emit(task.getOperatorId(), "task-update", task);
        }
    }
}
