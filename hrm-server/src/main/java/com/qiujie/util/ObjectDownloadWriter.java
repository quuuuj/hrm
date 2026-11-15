package com.qiujie.util;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;
import com.qiujie.common.dto.Response;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.common.storage.MinioStorageService;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 对象下载响应写出：文件中心与知识库文档共用同一套下载语义。
 * <p>
 * 关键点是错误响应不能走 {@code sendError}：容器 ERROR dispatch 会重入安全过滤器链，
 * 而 OncePerRequestFilter 默认跳过 ERROR dispatch，JwtAuthenticationFilter 不参与 →
 * SecurityContext 为空 → 未认证处理器把响应改写成 200 + 1200「认证失败」，真实 404 语义被吃掉。
 */
public final class ObjectDownloadWriter {

    private ObjectDownloadWriter() {
    }

    /**
     * 把对象以附件形式流式写入响应；存储数据若为 zstd 压缩帧则边读边解压。
     *
     * @param storageService MinIO 服务
     * @param key            对象键
     * @param response       HTTP 响应
     */
    public static void write(MinioStorageService storageService, String key,
                             HttpServletResponse response) throws IOException {
        if (!storageService.exists(key)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(JSON.toJSONString(Response.error(BusinessStatusEnum.FILE_NOT_EXIST)));
            return;
        }

        response.addHeader("Content-Type", "application/octet-stream;charset=utf-8");
        response.addHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(key, StandardCharsets.UTF_8));

        InputStream rawStream = storageService.get(key);

        try (InputStream in = rawStream) {
            // 探测是否压缩数据
            byte[] header = StorageCompressor.peekMagicBytes(in);
            OutputStream out = response.getOutputStream();

            if (header.length == 4
                    && header[0] == (byte) 0x28 && header[1] == (byte) 0xB5
                    && header[2] == 0x2F && header[3] == (byte) 0xFD) {
                // 压缩数据：流式解压输出；原始字节数未落库，不设 Content-Length，交给分块传输
                // 拼接回 magic bytes + 剩余流，再解压
                try (ByteArrayInputStream headerStream = new ByteArrayInputStream(header);
                     InputStream fullStream = new java.io.SequenceInputStream(headerStream, in);
                     InputStream decompressed = StorageCompressor.decompressStream(fullStream)) {
                    IoUtil.copy(decompressed, out, 8192);
                }
            } else {
                // 未压缩数据：拼接 header + 剩余流直接输出
                out.write(header);
                IoUtil.copy(in, out, 8192);
            }
            out.flush();
        }
    }
}
