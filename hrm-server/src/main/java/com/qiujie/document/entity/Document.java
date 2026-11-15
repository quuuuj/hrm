package com.qiujie.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体 (MySQL)
 * <p>
 * 与文件中心的 {@code sys_docs} 是两张独立的表：同一个物理对象不会同时被两行引用，
 * 因此每张表各自持有对象键唯一约束，删除时不需要跨表核对引用。
 *
 * @author quuj
 */
@Data
@Accessors(chain = true)
@TableName("kb_document")
@Schema(description = "知识库文档")
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** MinIO 对象键（物理文件引用键） */
    @Schema(description = "对象键")
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

    @Schema(description = "上传者id")
    @TableField("staff_id")
    private Integer staffId;

    /** 摄入状态：UPLOADED/PROCESSING/READY/FAILED */
    @Schema(description = "处理状态")
    @TableField("status")
    private String status;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "修改时间")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
