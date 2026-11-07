package com.qiujie.filetask.controller;

import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.filetask.service.FileTaskService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 文件任务查询与下载入口。
 * 任务创建分属各业务模块（/{module}/import/task、/{module}/export/task），
 * 分片上传在 {@code /file-upload}，本控制器只做查询与取文件。
 */
@RestController
@RequestMapping("/file-task")
public class FileTaskController {

    @Autowired
    private FileTaskService fileTaskService;

    @Operation(summary = "查询文件任务")
    @GetMapping
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String taskType,
                            String module) {
        return this.fileTaskService.list(current, size, taskType, module);
    }

    @Operation(summary = "查询文件任务详情")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Long id) {
        return this.fileTaskService.inspect(id);
    }

    @Operation(summary = "查询导入错误明细")
    @GetMapping("/{id}/errors")
    public ResponseDTO queryErrors(@PathVariable Long id,
                                   @RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return this.fileTaskService.queryErrors(id, current, size);
    }

    @Operation(summary = "下载任务文件")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, @RequestParam(defaultValue = "RESULT") String fileType,
                         HttpServletResponse response) throws IOException {
        this.fileTaskService.download(id, fileType, response);
    }
}
