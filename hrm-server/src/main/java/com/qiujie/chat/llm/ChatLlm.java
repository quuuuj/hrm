package com.qiujie.chat.llm;

import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 统一问答 LLM 调用领域端口（Port）。
 * <p>
 * 遵循 RFC #69：收拢为单一 ChatClient，承担主对话与轻量提示（意图识别/改写）。
 * 删除工具体系与 L1/L2 摘要方法。
 * </p>
 */
public interface ChatLlm {

    /**
     * 主对话调用。
     *
     * @param historyMessages 历史消息窗口（已按字符预算截断）
     * @param userMessage     当前用户输入
     * @param systemPrompt    系统提示词（含员工档案注入或检索上下文）
     * @return LLM 回复文本；失败或空返回 null
     */
    @Nullable
    String chat(List<Message> historyMessages, String userMessage, @Nullable String systemPrompt);

    /**
     * 轻量单次文本调用（用于意图闸门、查询改写等短提示）。
     *
     * @param prompt 待发送的提示文本
     * @return 模型返回内容；失败返回 null
     */
    @Nullable
    String call(String prompt);
}
