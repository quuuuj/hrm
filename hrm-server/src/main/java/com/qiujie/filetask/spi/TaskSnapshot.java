package com.qiujie.filetask.spi;

import com.qiujie.filetask.entity.FileTask;

/** 异步任务提交后的初始快照。 */
public record TaskSnapshot(Long taskId, FileTask snapshot) {
}
