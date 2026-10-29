package com.qiujie.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 统一给 Redis key 加项目前缀（`<application.name>:`），用于共享实例上的 key 隔离。
 * 挂在 KeySerializer 上后，opsForValue / opsForSet / boundXxxOps 等所有 key 操作自动带前缀，
 * 业务代码仍按裸 key 书写（如 `validate:code` → 实际写入 `hrm:validate:code`）。
 */
public class RedisKeyPrefixSerializer implements RedisSerializer<String> {

    private final StringRedisSerializer delegate = new StringRedisSerializer();

    private final String prefix;

    public RedisKeyPrefixSerializer(String applicationName) {
        this.prefix = StringUtils.hasText(applicationName) ? applicationName + ":" : "";
    }

    @Override
    public byte[] serialize(String key) throws SerializationException {
        if (key == null) {
            return null;
        }
        return delegate.serialize(prefix + key);
    }

    @Override
    public String deserialize(byte[] bytes) throws SerializationException {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }
}
