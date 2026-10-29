package com.qiujie.chat.llm;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * {@link ChatLlm} 的 Spring AI 实现——全模块唯一的 ChatClient。
 */
@Component
public class SpringAiChatLlm implements ChatLlm {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatLlm.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
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
    public String chatStream(List<Message> historyMessages, String userMessage, @Nullable String systemPrompt,
                             Consumer<String> onDelta) {
        StringBuilder answer = new StringBuilder();
        try {
            var spec = chatClient.prompt()
                    .messages(historyMessages == null ? List.of() : historyMessages)
                    .user(userMessage);
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                spec = spec.system(s -> s.text(systemPrompt));
            }
            for (String delta : spec.stream().content().toIterable()) {
                if (delta == null || delta.isEmpty()) {
                    continue;
                }
                answer.append(delta);
                onDelta.accept(delta);
            }
        } catch (Exception e) {
            // 已推送的片段用户已经看到，返回半成品由上层决定如何收尾，避免界面与库内不一致
            if (answer.length() == 0) {
                log.error("ChatLlm chatStream failed", e);
                return null;
            }
            log.warn("ChatLlm chatStream interrupted after {} chars: {}", answer.length(), e.getMessage());
        }
        return answer.length() == 0 ? null : answer.toString();
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
