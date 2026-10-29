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

    /** 对话窗口字符预算（ConversationWindow 截断阈值） */
    private int windowMaxChars = 6000;

    /** 知识作答上下文最大字符数 */
    private int maxContextChars = 3000;

    @PostConstruct
    public void validate() {
        log.info("Chat service ready (Spring AI / DashScope), windowMaxChars={}, maxContextChars={}",
                windowMaxChars, maxContextChars);
    }
}
