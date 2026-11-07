package com.qiujie.filetask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiujie.filetask.enums.UploadSessionStatusEnum;
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

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

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

    /** INIT / UPLOADING / COMPLETING / COMPLETED */
    @TableField("status")
    private UploadSessionStatusEnum status;

    @TableField("merged_object_key")
    private String mergedObjectKey;

    /** handler 版 complete 首次执行时返回的 JSON 快照，用于重复调用幂等返回 */
    @TableField("complete_result")
    private String completeResult;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
