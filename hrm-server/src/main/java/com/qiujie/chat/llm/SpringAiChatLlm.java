package com.qiujie.chat.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link ChatLlm} 的 Spring AI 实现——全模块唯一的 ChatClient。
 */
@Component
public class SpringAiChatLlm implements ChatLlm {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatLlm.class);

    private final ChatClient chatClient;

    public SpringAiChatLlm(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    @Nullable
    public String chat(List<Message> historyMessages, String userMessage, @Nullable String systemPrompt) {
        try {
            var spec = chatClient.prompt()
                    .messages(historyMessages == null ? List.of() : historyMessages)
                    .user(userMessage);
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                spec = spec.system(s -> s.text(systemPrompt));
            }
            String content = spec.call().content();
            return (content == null || content.isBlank()) ? null : content;
        } catch (Exception e) {
            log.error("ChatLlm chat failed", e);
            return null;
        }
    }

    @Override
    @Nullable
    public String call(String prompt) {
        try {
            String content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return (content == null || content.isBlank()) ? null : content.trim();
        } catch (Exception e) {
            log.warn("ChatLlm call failed: {}", e.getMessage());
            return null;
        }
    }
}
