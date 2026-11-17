package com.qiujie.document.controller;

import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.document.service.DocumentService;
import com.qiujie.document.service.DocumentUploadCompletionHandler;
import com.qiujie.filetask.service.FileUploadService;
import com.qiujie.staff.service.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 知识库文档接口。
 * <p>
 * 与文件中心（{@code /docs}）是两套独立的接口、实体与表；权限串用 kb:document:*。
 */
@RestController
@RequestMapping("/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private DocumentUploadCompletionHandler documentHandler;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('kb:document:list')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String oldName,
                            String status) {
        return this.documentService.list(current, size, oldName, status);
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('kb:document:delete')")
    public ResponseDTO delete(@PathVariable Long id) {
        return this.documentService.delete(id);
    }

    @Operation(summary = "批量删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('kb:document:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Long> ids) {
        return this.documentService.deleteBatch(ids);
    }

    @Operation(summary = "查看文档切片")
    @GetMapping("/{id}/chunks")
    @PreAuthorize("hasAnyAuthority('kb:document:list')")
    public ResponseDTO chunks(@PathVariable Long id) {
        return this.documentService.chunks(id);
    }

    @Operation(summary = "重新摄入文档（处理中除外）")
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyAuthority('kb:document:upload')")
    public ResponseDTO retry(@PathVariable Long id) {
        return this.documentService.retry(id);
    }

    @Operation(summary = "下载原始文件")
    // 对象键形如 kb/{staffId}/{uploadId}/{uuid}.{ext}，含斜杠，需用通配捕获整段路径
    @GetMapping("/download/{*filename}")
    @PreAuthorize("hasAnyAuthority('kb:document:list')")
    public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        // 通配捕获值带前导斜杠，去掉后才是对象键
        this.documentService.download(filename.startsWith("/") ? filename.substring(1) : filename, response);
    }

    @Operation(summary = "分片上传-合并分片并提交知识库文档")
    @PostMapping("/upload-task")
    @PreAuthorize("hasAnyAuthority('kb:document:upload')")
    public ResponseDTO completeUpload(@RequestParam String uploadId) {
        return fileUploadService.completeUpload(uploadId, documentHandler,
                securityUtil.getCurrentOperatorId());
    }
}
