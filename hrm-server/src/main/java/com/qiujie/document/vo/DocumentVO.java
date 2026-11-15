package com.qiujie.document.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档列表投影（kb_document join sys_staff）。
 */
@Data
public class DocumentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "对象键")
    private String name;

    @Schema(description = "文件原名称")
    private String oldName;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件大小kB")
    private Long size;

    @Schema(description = "上传者id")
    private Integer staffId;

    @Schema(description = "上传者姓名")
    private String staffName;

    @Schema(description = "处理状态：UPLOADED/PROCESSING/READY/FAILED")
    private String status;

    @Schema(description = "处理失败原因")
    private String failureReason;

    @Schema(description = "切片数量")
    private Integer chunkCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "上传完成时间")
    private LocalDateTime uploadTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
