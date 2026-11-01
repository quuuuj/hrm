package com.qiujie.filetask.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上传分片记录实体 (MySQL)
 *
 * @author quuj
 */
@Data
@Accessors(chain = true)
@TableName("file_upload_chunk")
public class FileUploadChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("upload_id")
    private String uploadId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("chunk_size")
    private Long chunkSize;

    @TableField("chunk_hash")
    private String chunkHash;

    @TableField("bucket")
    private String bucket;

    @TableField("storage_path")
    private String storagePath;

    @TableField("upload_time")
    private LocalDateTime uploadTime;
}
