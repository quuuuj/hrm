package com.qiujie.docs.controller;

import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.docs.entity.Docs;
import com.qiujie.docs.service.DocsService;
import com.qiujie.docs.service.DocsUploadCompletionHandler;
import com.qiujie.filetask.service.FileUploadService;
import com.qiujie.staff.service.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 文件中心接口（普通附件）。
 * <p>
 * 知识库文档是独立模块，接口在 {@code /document}。
 *
 * @Author qiujie
 * @Date 2022/2/24
 * @Version 1.0
 */
@RestController
@RequestMapping("/docs")
public class DocsController {

    @Autowired
    private DocsService docsService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private DocsUploadCompletionHandler docsHandler;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "新增")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO add(@RequestBody Docs docs) {
        return this.docsService.add(docs);
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO delete(@PathVariable Integer id) {
        return this.docsService.delete(id);
    }

    @Operation(summary = "批量删除")
    @DeleteMapping("/batch/{ids}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.docsService.deleteBatch(ids);
    }

    @Operation(summary = "编辑更新")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('system:docs:edit')")
    public ResponseDTO edit(@RequestBody Docs docs) {
        return this.docsService.edit(docs);
    }

    @Operation(summary = "查询")
    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Integer id) {
        return this.docsService.query(id);
    }

    @Operation(summary = "分页条件查询")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:docs:list','system:docs:search')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String oldName,
                            String staffName) {
        return this.docsService.list(current, size, oldName, staffName);
    }

    @Operation(summary = "数据导出接口")
    @GetMapping("/export/{filename}")
    @PreAuthorize("hasAnyAuthority('system:docs:export')")
    public void export(HttpServletResponse response, @PathVariable String filename) throws IOException {
        this.docsService.export(response, filename);
    }

    @Operation(summary = "数据导入接口")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('system:docs:import')")
    public ResponseDTO imp(MultipartFile file) throws IOException {
        return this.docsService.imp(file);
    }

    @Operation(summary = "文件下载")
    // 对象键形如 docs/{staffId}/{uploadId}/{uuid}.{ext}，含斜杠，需用通配捕获整段路径
    @GetMapping("/download/{*filename}")
    @PreAuthorize("hasAnyAuthority('system:docs:download')")
    public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        // 通配捕获值带前导斜杠，去掉后才是对象键
        this.docsService.download(filename.startsWith("/") ? filename.substring(1) : filename, response);
    }

    @Operation(summary = "头像下载")
    @GetMapping("/avatar/{filename}")
    public void getAvatar(@PathVariable String filename, HttpServletResponse response) throws IOException {
        this.docsService.download(filename, response);
    }

    // ========== 分片上传（合并由本接口完成，分片协议在 /file-upload）==========

    @Operation(summary = "头像上传（小文件直传）")
    @PostMapping("/upload/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO upload(MultipartFile file, @PathVariable Integer id) throws IOException {
        return this.docsService.upload(file, id);
    }

    @Operation(summary = "分片上传-合并分片并登记普通附件")
    @PostMapping("/upload-task")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO completeUpload(@RequestParam String uploadId) {
        return fileUploadService.completeUpload(uploadId, docsHandler,
                securityUtil.getCurrentOperatorId());
    }
}
