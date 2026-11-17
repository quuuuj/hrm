package com.qiujie.common.storage;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MinIO S3 对象存储服务。
 * 唯一存储后端，所有文件操作均通过此服务。
 *
 * @author qiujie
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.bucket}")
    private String bucket;

    private MinioClient client;

    @PostConstruct
    void init() {
        try {
            this.client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
            log.info("MinIO connected: endpoint={}, bucket={}", endpoint, bucket);
        } catch (Exception e) {
            log.error("MinIO init failed: endpoint={}", endpoint, e);
            throw new RuntimeException("MinIO 初始化失败，请检查 storage.minio 配置", e);
        }
    }

    public InputStream get(String key) {
        Exception lastEx = null;
        for (int i = 0; i < 3; i++) {
            try {
                return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
            } catch (Exception e) {
                lastEx = e;
                if (i < 2) {
                    try { Thread.sleep(200L * (i + 1)); } catch (InterruptedException ignored) {}
                }
            }
        }
        throw new RuntimeException("MinIO get failed: " + key, lastEx);
    }

    public void put(String key, InputStream inputStream, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(inputStream, -1, 5 * 1024 * 1024L)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO put failed: " + key, e);
        }
    }

    public void put(String key, byte[] bytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(in, bytes.length, -1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO put failed: " + key, e);
        }
    }

    /**
     * 合并多个源对象为一个目标对象，使用服务端 compose API（零拷贝）。
     * 单分片时直接 copyObject。
     */
    public void composeObject(List<String> sourceKeys, String targetKey, String contentType) {
        if (sourceKeys.size() == 1) {
            try {
                client.copyObject(CopyObjectArgs.builder()
                        .bucket(bucket).object(targetKey)
                        .source(CopySource.builder().bucket(bucket).object(sourceKeys.get(0)).build())
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("MinIO copy failed: " + targetKey, e);
            }
            return;
        }
        try {
            List<ComposeSource> sources = sourceKeys.stream()
                    .map(k -> ComposeSource.builder().bucket(bucket).object(k).build())
                    .collect(Collectors.toList());
            client.composeObject(ComposeObjectArgs.builder()
                    .bucket(bucket).object(targetKey)
                    .sources(sources)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO compose failed: " + targetKey, e);
        }
    }

    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO delete failed: " + key, e);
        }
    }

    /**
     * 批量删除对象（S3 DeleteObjects 协议，SDK 内部按 1000 个/请求自动分片）。
     * <p>
     * 与 {@link #delete} 语义不同：批量接口不抛单个对象的失败——不存在的对象视为成功，
     * 失败项收集后返回，由调用方决定记日志还是忽略。一批里失败一个不该让其余已成功的
     * 操作回滚（对象存储本来也回滚不了）。
     * <p>
     * SDK 返回的是惰性 {@link Iterable}，删除请求在遍历时才真正发出：不遍历等于什么都没删。
     *
     * @return 删除失败的 key（空集表示全部成功）；整批请求本身失败时返回全部 key
     */
    public Set<String> deleteAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }
        List<DeleteObject> objects = keys.stream().map(DeleteObject::new).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results = client.removeObjects(
                RemoveObjectsArgs.builder().bucket(bucket).objects(objects).build());
        Set<String> failed = new HashSet<>();
        try {
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                failed.add(error.objectName());
                log.warn("MinIO 批量删除单个对象失败: key={}, code={}, message={}",
                        error.objectName(), error.code(), error.message());
            }
        } catch (Exception e) {
            // 整批请求失败（网络/鉴权）：无法区分哪些已删，全部计为失败，交由孤儿清理兜底
            log.error("MinIO 批量删除请求失败: keys={}", keys, e);
            return new HashSet<>(keys);
        }
        return failed;
    }

    public boolean exists(String key) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> listKeys() {
        List<String> keys = new ArrayList<>();
        try {
            Iterable<Result<io.minio.messages.Item>> results = client.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).build());
            for (Result<io.minio.messages.Item> result : results) {
                keys.add(result.get().objectName());
            }
        } catch (Exception e) {
            log.error("MinIO list failed", e);
        }
        return keys;
    }

    public Date getLastModified(String key) {
        try {
            var stat = client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return Date.from(stat.lastModified().toInstant());
        } catch (Exception e) {
            return null;
        }
    }
}
