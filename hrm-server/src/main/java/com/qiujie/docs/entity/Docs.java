package com.qiujie.docs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 文件中心附件实体 (MySQL，sys_docs)。
 * <p>
 * 只承载普通附件与头像；知识库文档是独立的 {@code kb_document} 表，由 document 模块负责。
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "修改时间")
    @TableField("update_time")
    private Timestamp updateTime;
}
