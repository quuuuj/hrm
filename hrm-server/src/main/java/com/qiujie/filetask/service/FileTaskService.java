package com.qiujie.filetask.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.filetask.engine.FileTaskEngine;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.enums.TaskFileTypeEnum;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.enums.TaskTypeEnum;
import com.qiujie.filetask.mapper.FileTaskMapper;
import com.qiujie.filetask.spi.ExportProcessor;
import com.qiujie.filetask.spi.ImportProcessor;
import com.qiujie.filetask.store.FileTaskStore;
import com.qiujie.filetask.store.MinioArtifactStore;
import com.qiujie.staff.service.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件任务统一入口：任务创建与异步提交、查询、访问控制与文件下载。
 * <p>
 * 业务模块只描述导入/导出的处理器，任务生命周期由本类持有，持久化与状态流转委托给
 * {@link FileTaskStore}，执行委托给 {@link FileTaskEngine}。
 * </p>
 */
@Service
public class FileTaskService {

    @Autowired
    private FileTaskStore taskStore;
    @Autowired
    private FileTaskMapper fileTaskMapper;
    @Autowired
    private FileTaskErrorService fileTaskErrorService;
    @Autowired
    private MinioArtifactStore artifactStore;
    @Autowired
    private FileTaskEngine fileTaskEngine;
    @Autowired
    private SecurityUtil securityUtil;
    @Autowired
    @Qualifier("fileTaskExecutor")
    private ThreadPoolTaskExecutor fileTaskExecutor;

    // ==================== 任务提交 ====================

    /**
     * 创建导入任务并提交异步执行。
     *
     * @param sourceFilePath 已完成分片合并的源文件 key
     * @param queryParams    业务自定义参数，可为 null
     * @return 任务初始快照，状态为 PENDING
     */
    public <T> FileTask submitImport(TaskModuleEnum module, String fileName, String sourceFilePath,
                                     String queryParams, Integer operatorId, ImportProcessor<T> processor) {
        FileTask task = taskStore.create(TaskTypeEnum.IMPORT, module, fileName,
                sourceFilePath, queryParams, operatorId);
        fileTaskExecutor.execute(() -> fileTaskEngine.runImport(task.getId(), processor));
        return task;
    }

    /** 创建导出任务并提交异步执行。 */
    public <T> FileTask submitExport(TaskModuleEnum module, String fileName, String queryParams,
                                     Integer operatorId, ExportProcessor<T> processor) {
        FileTask task = taskStore.create(TaskTypeEnum.EXPORT, module, fileName,
                null, queryParams, operatorId);
        fileTaskExecutor.execute(() -> fileTaskEngine.runExport(task.getId(), processor, queryParams, fileName));
        return task;
    }

    // ==================== 查询与下载 ====================

    public ResponseDTO list(Integer current, Integer size, String taskType, String module) {
        QueryWrapper<FileTask> queryWrapper = new QueryWrapper<>();
        Integer operatorId = getCurrentOperatorId();
        if (operatorId != null) {
            queryWrapper.eq("operator_id", operatorId);
        }
        if (taskType != null && !taskType.isEmpty()) {
            queryWrapper.eq("task_type", taskType);
        }
        if (module != null && !module.isEmpty()) {
            queryWrapper.eq("module", module);
        }
        queryWrapper.orderByDesc("id");
        IPage<FileTask> page = fileTaskMapper.selectPage(new Page<>(current, size), queryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    public ResponseDTO inspect(Long id) {
        FileTask fileTask = fileTaskMapper.selectById(id);
        if (fileTask == null) {
            return Response.error("任务不存在");
        }
        if (!canAccess(fileTask)) {
            return Response.error("无权访问该任务");
        }
        return Response.success(fileTask);
    }

    public ResponseDTO queryErrors(Long taskId, Integer current, Integer size) {
        FileTask fileTask = fileTaskMapper.selectById(taskId);
        if (fileTask == null) {
            return Response.error("任务不存在");
        }
        if (!canAccess(fileTask)) {
            return Response.error("无权访问该任务");
        }
        QueryWrapper<FileTaskError> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).orderByAsc("row_num");
        IPage<FileTaskError> page = fileTaskErrorService.page(new Page<>(current, size), queryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    public void download(Long id, String fileType, HttpServletResponse response) throws IOException {
        FileTask fileTask = fileTaskMapper.selectById(id);
        if (fileTask == null) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "任务不存在");
            return;
        }
        if (!canAccess(fileTask)) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "无权访问该任务");
            return;
        }
        String key = resolveDownloadKey(fileTask, fileType);
        if (key == null || key.isEmpty()) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }
        if (!artifactStore.exists(key)) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "文件不存在或已被清理");
            return;
        }
        String downloadName = resolveDownloadName(fileTask, fileType);
        response.addHeader("Content-Type", "application/octet-stream;charset=utf-8");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8));
        try (InputStream in = artifactStore.open(key);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    // ==================== 内部工具 ====================

    private boolean canAccess(FileTask fileTask) {
        Integer operatorId = getCurrentOperatorId();
        return operatorId == null || fileTask.getOperatorId() == null || operatorId.equals(fileTask.getOperatorId());
    }

    private Integer getCurrentOperatorId() {
        return securityUtil.getCurrentOperatorId();
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(JSON.toJSONString(Response.error(message)));
    }

    private String resolveDownloadKey(FileTask fileTask, String fileType) {
        if (TaskFileTypeEnum.SOURCE.getValue().equalsIgnoreCase(fileType)) {
            return fileTask.getSourceFilePath();
        }
        if (TaskFileTypeEnum.ERROR.getValue().equalsIgnoreCase(fileType)) {
            return fileTask.getErrorFilePath();
        }
        return fileTask.getResultFilePath();
    }

    private String resolveDownloadName(FileTask fileTask, String fileType) {
        if (TaskFileTypeEnum.ERROR.getValue().equalsIgnoreCase(fileType)) {
            return "import-errors.xlsx";
        }
        String name = fileTask.getFileName();
        return name != null && !name.isEmpty() ? name : "download";
    }
}
