package com.qiujie.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.username}") String username,
            @Value("${spring.redis.password}") String password) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        // 共享实例按 ACL 用户隔离（key 前缀 hrm:）；本地单实例无用户名，留空即走 default 用户
        if (StringUtils.hasText(username)) {
            config.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            config.setPassword(RedisPassword.of(password));
        }
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP2)
                        .build())
                .build();
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
                                                       @Value("${spring.application.name}") String applicationName) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(factory);
        // key 统一带 `<application.name>:` 前缀，共享 Redis 实例按项目隔离
        RedisKeyPrefixSerializer keyPrefixSerializer = new RedisKeyPrefixSerializer(applicationName);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance , ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        redisTemplate.setKeySerializer(keyPrefixSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(keyPrefixSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 覆盖 Spring Boot 自动装配的 StringRedisTemplate，同样带上项目前缀，
     * 避免限流等直接用 StringRedisTemplate 的地方绕开隔离。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory,
                                                   @Value("${spring.application.name}") String applicationName) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(factory);
        stringRedisTemplate.setKeySerializer(new RedisKeyPrefixSerializer(applicationName));
        stringRedisTemplate.afterPropertiesSet();
        return stringRedisTemplate;
    }
}
