package com.qiujie.filetask.spi;

/**
 * 业务模块提交异步文件任务的唯一入口。
 * 任务生命周期、文件介质和通知细节由实现隐藏。
 */
public interface AsyncFileTasks {

    <T> TaskSnapshot submitImport(ImportRequest<T> request);

    <T> TaskSnapshot submitExport(ExportRequest<T> request);

    record ImportRequest<T>(com.qiujie.filetask.enums.TaskModuleEnum module,
                            String fileName,
                            String sourceFilePath,
                            String queryParams,
                            Integer operatorId,
                            ImportProcessor<T> processor) {
    }

    record ExportRequest<T>(com.qiujie.filetask.enums.TaskModuleEnum module,
                            String fileName,
                            String queryParams,
                            Integer operatorId,
                            ExportProcessor<T> processor) {
    }
}
