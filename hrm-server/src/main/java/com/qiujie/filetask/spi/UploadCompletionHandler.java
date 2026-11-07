package com.qiujie.filetask.spi;

import com.qiujie.filetask.enums.UploadScopeEnum;

import java.util.Collections;
import java.util.Map;

/**
 * 分片上传完成后的处理策略。
 * 知识库创建文档记录并触发 ETL，通用文档创建 Docs + 压缩去重。
 */
public interface UploadCompletionHandler {

    /** 该处理器负责的上传范围，init 阶段按此匹配秒传检测 */
    UploadScopeEnum getScope();

    /** 存储路径前缀 */
    String getStoragePrefix();

    /**
     * 秒传检测：本人名下已存在相同 hash 的文档时返回其信息。
     * <p>
     * 去重范围限定在当前操作人，不跨员工命中：秒传不创建新的业务行与对象，
     * 若命中他人文档，对方删除时该对象会被引用计数判定为无人引用而删除，
     * 秒传方随后拿到的是一个指向空对象的文档信息。
     *
     * @param fileHash 整文件 SHA-256
     * @param staffId  当前操作员工 ID，作为去重范围
     * @return 存在则返回 {instantUpload:true, ...}，不存在返回 null
     */
    default Map<String, Object> checkDedup(String fileHash, Integer staffId) {
        return Collections.emptyMap();
    }

    /**
     * 分片合并完成后执行的业务逻辑。
     * @param mergedKey  合并后对象的存储 key
     * @param session    上传会话信息
     * @return 返回给客户端的额外字段，可为 null
     */
    Map<String, Object> onComplete(String mergedKey, UploadSessionInfo session);
}
