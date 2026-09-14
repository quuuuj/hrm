package com.qiujie.knowledge.service;

import org.springframework.stereotype.Service;

/**
 * 文本清洗服务：去除解析后的文档中的噪声内容。
 */
@Service
public class TextCleanupService {

    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                // 移除 NUL 字节：PostgreSQL UTF-8 编码拒绝 0x00，
                // txt/md 二进制误传或编码检测失误时会残留并导致切片落库失败
                .replace("\u0000", "")
                // 统一换行为 \n
                .replace("\r\n", "\n").replace("\r", "\n")
                // 压缩 3+ 连续换行为双换行
                .replaceAll("\\n{3,}", "\n\n")
                // 移除独立的页码行
                .replaceAll("(?m)^\\s*\\d{1,4}\\s*$", "")
                // 压缩多余空白行
                .replaceAll("(?m)^\\s+$", "")
                // 压缩连续空白字符
                .replaceAll("[ \\t]{2,}", " ")
                // 移除页眉页脚类短行（< 5 字符且不含中英文）
                .replaceAll("(?m)^.{1,4}$", "")
                .trim();
    }
}
