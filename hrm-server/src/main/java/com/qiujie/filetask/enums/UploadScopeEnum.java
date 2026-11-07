package com.qiujie.filetask.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分片上传范围，决定 init 阶段是否做秒传检测。不落库，仅用于上传协议路由。
 * <p>
 * DOCS：文件中心 / 知识库上传，命中本人同 hash 文档时秒传；
 * IMPORT：考勤、薪资、加班等业务数据导入，不做秒传，合并后由业务接口创建任务。
 */
@Getter
@AllArgsConstructor
public enum UploadScopeEnum {

    DOCS("DOCS"),
    IMPORT("IMPORT");

    private final String value;

    /** 按请求体里的 scope 解析，未传或非法时返回 null */
    public static UploadScopeEnum of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (UploadScopeEnum scope : values()) {
            if (scope.value.equalsIgnoreCase(value.trim())) {
                return scope;
            }
        }
        return null;
    }
}
