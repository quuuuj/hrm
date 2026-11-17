package com.qiujie.document.util;

/**
 * 文本清洗工具：去除解析后的文档中的噪声内容。
 * <p>
 * 纯函数，无 Spring 依赖，可直接零 mock 单测。
 */
public final class TextCleanupUtil {

    public static String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                // 移除 NUL 字节：PostgreSQL UTF-8 编码拒绝 0x00，
                // txt/md 二进制误传或编码检测失误时会残留并导致切片落库失败
                .replace("\u0000", "")
                // 统一换行为 \n
                .replace("\r\n", "\n").replace("\r", "\n")
                // 移除独立的页码行
                .replaceAll("(?m)^\\s*\\d{1,4}\\s*$", "")
                // 压缩多余空白行
                .replaceAll("(?m)^\\s+$", "")
                // 压缩连续空白字符
                .replaceAll("[ \\t]{2,}", " ")
                // 移除页眉页脚类短行（< 5 字符且不含中英文）
                .replaceAll("(?m)^.{1,4}$", "")
                // 页码、短行移除后会留下新的空行，统一成双换行作为段落边界
                .replaceAll("\\n{3,}", "\n\n")
                // 合并段内软换行：PDF 按版面逐行抽取，正文行常从词中间断开，
                // 预览时撑出右侧空白，也让 embedding 文本断裂。
                // 空行（\n\n）是段落边界；Markdown 标题/列表等结构行不合并。
                .replaceAll("(?m)(^[^\\n#>*+\\-][^\\n]*?)[ \\t]*\\n[ \\t]*(?=[^\\s#>*\\-+0-9第])", "$1")
                .trim();
    }
}
