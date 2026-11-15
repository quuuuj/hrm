package com.qiujie.document.enums;

/**
 * 知识库文档处理状态。
 * UPLOADED → PROCESSING → READY / FAILED
 */
public enum DocumentStatusEnum {

    UPLOADED("已上传"),
    PROCESSING("处理中"),
    READY("就绪"),
    FAILED("失败");

    private final String description;

    DocumentStatusEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
