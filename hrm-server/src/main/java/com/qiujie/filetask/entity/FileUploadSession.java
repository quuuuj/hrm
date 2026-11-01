package com.qiujie.filetask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上传会话实体 (MySQL)
 *
 * @author quuj
 */
@Data
@Accessors(chain = true)
@TableName("file_upload_session")
public class FileUploadSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "upload_id", type = IdType.ASSIGN_UUID)
    private String uploadId;

    @TableField("staff_id")
    private Integer staffId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_ext")
    private String fileExt;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_hash")
    private String fileHash;

    @TableField("chunk_size")
    private Long chunkSize;

    @TableField("chunk_count")
    private Integer chunkCount;

    /** INIT / UPLOADING / COMPLETING / COMPLETED / EXPIRED */
    @TableField("status")
    private String status;

    @TableField("bucket")
    private String bucket;

    @TableField("merged_object_key")
    private String mergedObjectKey;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
