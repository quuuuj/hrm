package com.qiujie.chat;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Unified Chat module configuration.
 *
 * @author quuj
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private static final Logger log = LoggerFactory.getLogger(ChatProperties.class);

    private boolean enabled = true;

    private int timeoutSeconds = 15;

    /** 对话窗口字符预算（ConversationWindow 截断阈值） */
    private int windowMaxChars = 6000;

    /** 知识作答上下文最大字符数 */
    private int maxContextChars = 3000;

    private Provider provider = new Provider();

    @Data
    public static class Provider {
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
    }

    @PostConstruct
    public void validate() {
        if (!enabled) {
            log.info("Chat service is disabled");
            return;
        }
        log.info("Chat service enabled (using Spring AI OpenAPI/DashScope), windowMaxChars={}", windowMaxChars);
    }
}
