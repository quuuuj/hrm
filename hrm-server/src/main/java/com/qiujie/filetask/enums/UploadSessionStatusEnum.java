package com.qiujie.filetask.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分片上传会话状态。
 * INIT → UPLOADING → COMPLETING → COMPLETED；会话过期后直接由清理任务物理删除，不再单独设 EXPIRED。
 */
@Getter
@AllArgsConstructor
public enum UploadSessionStatusEnum {

    INIT("INIT"),
    UPLOADING("UPLOADING"),
    COMPLETING("COMPLETING"),
    COMPLETED("COMPLETED");

    @EnumValue
    private final String value;
}
