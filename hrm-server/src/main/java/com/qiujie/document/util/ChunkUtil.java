package com.qiujie.document.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片工具。按段落自然边界分割，兼顾 maxSize 和 overlap。
 * <p>
 * 纯函数：切片参数由调用方传入（{@code kb.chunk.*} 的读取留在消费方），
 * 无 Spring 依赖，可直接零 mock 单测。
 */
public final class ChunkUtil {

    /**
     * @param text    清洗后的全文
     * @param maxSize 最大切片字符数
     * @param overlap 相邻切片重叠字符数
     */
    public static List<ChunkResult> split(String text, int maxSize, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // 1. 按段落边界切分
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        int currentCharStart = 0;
        String overlapText = "";

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (current.length() + trimmed.length() > maxSize && current.length() > 0) {
                chunks.add(buildChunk(current.toString(), currentCharStart));
                // overlap: 保留上一块的结尾部分
                overlapText = current.substring(Math.max(0, current.length() - overlap));
                currentCharStart += current.length() - overlapText.length();
                current = new StringBuilder(overlapText + trimmed + "\n\n");
            } else {
                current.append(trimmed).append("\n\n");
            }
        }

        // 最后一个切片
        if (current.length() > 0) {
            chunks.add(buildChunk(current.toString(), currentCharStart));
        }

        return chunks;
    }

    private static ChunkResult buildChunk(String text, int charStart) {
        String trimmed = text.trim();
        // 估算 token 数：中文约1.5字符/token，英文约4字符/token
        int tokenCount = (int) (trimmed.length() / 2.5);
        return new ChunkResult(trimmed, tokenCount, charStart, charStart + trimmed.length());
    }

    public record ChunkResult(String text, int tokenCount, int charStart, int charEnd) {
    }
}
