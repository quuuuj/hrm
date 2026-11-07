package com.qiujie.filetask.engine;
import com.qiujie.filetask.service.FileTaskErrorService;
import com.qiujie.filetask.spi.ExportProcessor;
import com.qiujie.filetask.spi.ImportProcessor;
import com.qiujie.filetask.spi.ImportRow;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.enums.TaskStatusEnum;
import com.qiujie.filetask.notify.FileTaskNotifier;
import com.qiujie.filetask.store.ArtifactStore;
import com.qiujie.filetask.store.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用异步导入导出引擎。
 * 负责流式读写、缓冲管理、进度更新、错误文件生成等通用逻辑，
 * 具体业务的校验和入库逻辑由各模块通过 ImportProcessor / ExportProcessor 接口提供。
 *
 * @author qiujie
 */
@Service
public class FileTaskEngine {

    private static final Logger log = LoggerFactory.getLogger(FileTaskEngine.class);

    private static final int DB_BATCH_SIZE = 200;
    private static final int IMPORT_BATCH_SIZE = 500;
    private static final int HEAD_ROW_NUMBER = 2;
    private static final int EXPORT_PAGE_SIZE = 500;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FileTaskErrorService fileTaskErrorService;

    @Autowired
    private ArtifactStore artifactStore;

    @Autowired
    private FileTaskNotifier fileTaskNotifier;

    /**
     * 执行异步导入。
     * 流式读取源文件，每攒够一批行交给 processor 批量处理，任务结算统一在此完成。
     *
     * @param taskId    文件任务ID
     * @param processor 模块导入处理器
     */
    public <T> void runImport(Long taskId, ImportProcessor<T> processor) {
        if (!taskRepository.claimRunning(taskId)) {
            return;
        }
        FileTask task = taskRepository.getById(taskId);
        if (task == null) {
            return;
        }
        File sourceFile = null;
        boolean temporarySource = false;
        try {
            sourceFile = artifactStore.resolveSource(task.getSourceFilePath());
            temporarySource = artifactStore.isRemote(task.getSourceFilePath());
            List<T> rows = new ArrayList<>(IMPORT_BATCH_SIZE);
            EasyExcel.read(sourceFile, processor.getRowClass(), new AnalysisEventListener<T>() {
                @Override
                public void invoke(T data, AnalysisContext context) {
                    if (data instanceof ImportRow row) {
                        // EasyExcel 行下标从 0 开始，Excel 行号从 1 开始
                        row.setRowNum(context.readRowHolder().getRowIndex() + 1);
                    }
                    rows.add(data);
                    if (rows.size() >= IMPORT_BATCH_SIZE) {
                        processBatch(new ArrayList<>(rows), taskId, processor);
                        rows.clear();
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    if (!rows.isEmpty()) {
                        processBatch(new ArrayList<>(rows), taskId, processor);
                        rows.clear();
                    }
                }
            }).headRowNumber(HEAD_ROW_NUMBER).sheet().doRead();

            FileTask finishedTask = taskRepository.getById(taskId);
            if (finishedTask != null && finishedTask.getProcessedCount() != null) {
                // 流式读取完成后回填真实总量，前端进度显示准确
                taskRepository.setTotalCount(taskId, finishedTask.getProcessedCount());
            }
            if (finishedTask != null && finishedTask.getFailCount() != null && finishedTask.getFailCount() > 0) {
                taskRepository.generateErrorFile(taskId);
                taskRepository.finish(taskId, TaskStatusEnum.PARTIAL_SUCCESS);
            } else {
                taskRepository.finish(taskId, TaskStatusEnum.SUCCESS);
            }
        } catch (Exception e) {
            taskRepository.fail(taskId, e);
        } finally {
            deleteTemporaryFile(sourceFile, temporarySource);
            // 所有终态都清理源文件，避免敏感数据长期驻留
            try {
                taskRepository.deleteSourceFile(taskId);
            } catch (Exception e) {
                log.warn("Failed to delete source file: taskId={}", taskId, e);
            }
            fileTaskNotifier.onTaskTerminal(taskId);
        }
    }

    private <T> void processBatch(List<T> rows, Long taskId, ImportProcessor<T> processor) {
        List<FileTaskError> errors = new ArrayList<>();
        processor.processBatch(rows, taskId, errors::add);
        if (!errors.isEmpty()) {
            fileTaskErrorService.saveBatch(errors, DB_BATCH_SIZE);
        }
        taskRepository.increaseProgress(taskId, 0, rows.size(),
                rows.size() - errors.size(), errors.size());
    }

    /**
     * 执行异步导出。
     * 通过 processor 分页查询数据，增量写入 Excel，每页更新进度。
     *
     * @param taskId           文件任务ID
     * @param processor        模块导出处理器
     * @param queryParamsJson  查询参数 JSON
     * @param exportName       导出文件名
     */
    public <T> void runExport(Long taskId, ExportProcessor<T> processor, String queryParamsJson, String exportName) {
        if (!taskRepository.claimRunning(taskId)) {
            return;
        }
        File resultFile = null;
        ExcelWriter excelWriter = null;
        try {
            resultFile = artifactStore.createTaskFile("task-result", exportName);
            excelWriter = EasyExcel.write(resultFile, processor.getRowClass()).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("data").build();
            int current = 1;
            IPage<T> page;
            do {
                page = processor.queryPage(current, EXPORT_PAGE_SIZE, queryParamsJson);
                List<T> list = page.getRecords();
                if (list.isEmpty()) {
                    break;
                }
                excelWriter.write(list, writeSheet);
                if (current == 1) {
                    taskRepository.setTotalCount(taskId, (int) page.getTotal());
                }
                taskRepository.increaseProgress(taskId, 0, list.size(), list.size(), 0);
                current++;
            } while (current <= page.getPages());
            // EasyExcel 在 finish() 时才把缓冲区落盘，必须先关闭 writer 再上传
            excelWriter.finish();
            excelWriter = null;
            taskRepository.setResultFile(taskId, artifactStore.upload(resultFile, "task-result"));
            resultFile = null;
            taskRepository.finish(taskId, TaskStatusEnum.SUCCESS);
        } catch (Exception e) {
            taskRepository.fail(taskId, e);
        } finally {
            try {
                if (excelWriter != null) {
                    excelWriter.finish();
                }
            } finally {
                deleteTemporaryFile(resultFile, true);
                fileTaskNotifier.onTaskTerminal(taskId);
            }
        }
    }

    private void deleteTemporaryFile(File file, boolean temporary) {
        if (temporary && file != null && file.isFile() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    /**
     * 解析源文件已委托给 ArtifactStore。
     */
}
