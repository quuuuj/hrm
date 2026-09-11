package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author qiujie
 * @since 2022-02-24
 */
@Data
@Accessors(chain = true)
@TableName("sys_docs")
@Schema(description = "Docs对象 - 文件管理")
public class Docs implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "文件名称")
    @TableField("name")
    private String name;

    @Schema(description = "文件类型")
    @TableField("type")
    private String type;

    @Schema(description = "文件原名称")
    @TableField("old_name")
    private String oldName;

    @Schema(description = "文件SHA256哈希")
    @TableField("file_hash")
    private String fileHash;

    @Schema(description = "文件原始大小kB")
    @TableField("size")
    private Long size;

    @Schema(description = "磁盘实际占用(字节)")
    @TableField("stored_size")
    private Long storedSize;

    @Schema(description = "是否zstd压缩: 0=否 1=是")
    @TableField("compressed")
    private Integer compressed;

    @Schema(description = "文件上传者id")
    @TableField("staff_id")
    private Integer staffId;

    @Schema(description = "员工备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "修改时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "0未删除，1已删除，默认为0")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

    // ===== 知识库字段（sys_docs 吸收 kb_document）=====

    /** 知识库状态：UPLOADED/PROCESSING/READY/FAILED；NULL=未入库（通用文件）。 */
    @Schema(description = "知识库状态")
    @TableField("kb_status")
    private String kbStatus;

    @Schema(description = "处理失败原因")
    @TableField("failure_reason")
    private String failureReason;

    @Schema(description = "文档预览文本")
    @TableField("preview_text")
    private String previewText;

    @Schema(description = "切片数量")
    @TableField("chunk_count")
    private Integer chunkCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "上传完成时间")
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "处理完成时间")
    @TableField("process_time")
    private LocalDateTime processTime;
}
