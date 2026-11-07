package com.qiujie.filetask.controller;

import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.filetask.enums.UploadScopeEnum;
import com.qiujie.filetask.service.FileUploadService;
import com.qiujie.filetask.spi.UploadCompletionHandler;
import com.qiujie.staff.service.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 分片上传协议入口：初始化会话 + 上传分片。
 * <p>
 * 文件中心与业务导入共用同一套协议，区别只在 init 请求体的 scope：
 * DOCS 走秒传检测（合并由 /docs/upload-task 完成），IMPORT 不秒传（合并由各业务模块的
 * /{module}/import/task 完成）。合并与业务落库都留在调用方，本控制器不感知业务。
 */
@RestController
@RequestMapping("/file-upload")
public class FileUploadController {

    private static final String UPLOAD_AUTHORITIES =
            "hasAnyAuthority('system:docs:upload','kb:knowledge:upload',"
                    + "'performance:attendance:import','performance:overtime:import','money:salary:import')";

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private SecurityUtil securityUtil;

    /** 各业务模块实现的完成处理器，按各自声明的 scope 匹配；IMPORT 无处理器即跳过秒传 */
    @Autowired
    private List<UploadCompletionHandler> completionHandlers;

    @Operation(summary = "分片上传-初始化")
    @PostMapping("/init")
    @PreAuthorize(UPLOAD_AUTHORITIES)
    public ResponseDTO initUpload(@RequestBody Map<String, Object> request) {
        UploadScopeEnum scope = UploadScopeEnum.of((String) request.get("scope"));
        if (scope == null) {
            return Response.error("上传范围不合法");
        }
        String fileName = (String) request.get("fileName");
        String fileExt = (String) request.get("fileExt");
        Long fileSize = toLong(request.get("fileSize"));
        String fileHash = (String) request.get("fileHash");
        Long chunkSize = toLong(request.get("chunkSize"));

        return fileUploadService.initUpload(fileName, fileExt, fileSize, fileHash, chunkSize,
                dedupHandler(scope), securityUtil.getCurrentOperatorId());
    }

    @Operation(summary = "分片上传-上传分片")
    @PostMapping("/chunks")
    @PreAuthorize(UPLOAD_AUTHORITIES)
    public ResponseDTO uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam(value = "chunkHash", required = false) String chunkHash,
            @RequestParam("chunk") MultipartFile chunk) {
        return fileUploadService.uploadChunk(uploadId, chunkIndex, chunkHash, chunk,
                securityUtil.getCurrentOperatorId());
    }

    /** 按 scope 找对应处理器，没有则是该 scope 不做秒传 */
    private UploadCompletionHandler dedupHandler(UploadScopeEnum scope) {
        return completionHandlers.stream()
                .filter(handler -> handler.getScope() == scope)
                .findFirst()
                .orElse(null);
    }

    /** 把请求体里的数值读成 Long，缺参或类型不符时返回 null，交由 service 统一报错 */
    private static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
