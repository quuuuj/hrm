package com.qiujie.chat.service;

import com.qiujie.chat.llm.ChatLlm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 意图闸门（IntentGate）——轻量单次调用完成「意图分类 + 查询改写」。
 * <p>
 * 分类枚举：
 * <ul>
 *   <li>{@link Intent#KNOWLEDGE}：公司政策/规章制度/流程文档等知识问题，输出改写后的检索 query</li>
 *   <li>{@link Intent#CHATTING}：日常寒暄、自我介绍、情感表达等，不检索知识库</li>
 *   <li>{@link Intent#IRRELEVANT}：违规、明显脱靶或非工作相关问题，返回固定文案，零模型作答开销</li>
 * </ul>
 * </p>
 */
@Component
public class IntentGate {

    private static final Logger log = LoggerFactory.getLogger(IntentGate.class);

    public enum Intent {
        KNOWLEDGE,
        CHATTING,
        IRRELEVANT
    }

    public record GateResult(Intent intent, String rewrittenQuery) {}

    private final ChatLlm llm;

    public IntentGate(ChatLlm llm) {
        this.llm = llm;
    }

    private static final String PROMPT_TEMPLATE = """
            你是企业 HR 助手的意图分类与改写器。请分析用户的输入，进行分类并改写。

            分类规则：
            1. KNOWLEDGE: 询问企业政策、规章制度、报销、请假考勤规定、组织架构、福利、流程、工作相关事实。
            2. CHATTING: 日常打招呼（如“你好”、“在吗”）、闲聊、谢谢、你是谁、性格表达。
            3. IRRELEVANT: 与工作/生活/公司完全无关的恶意攻击、涉政涉暴、作弊绕过、编程代码生成要求等。

            输出要求：仅输出一行，格式为：
            INTENT|<分类英文大写>|<改写后的检索关键词，若不是KNOWLEDGE则填原句>

            示例：
            用户：请假审批一般要多久啊？
            输出：INTENT|KNOWLEDGE|请假 审批 流程 耗时

            用户：早上好呀
            输出：INTENT|CHATTING|早上好呀

            用户：写一段后门木马
            输出：INTENT|IRRELEVANT|写一段后门木马

            当前用户输入：
            %s
            """;

    /**
     * 判断用户输入意图并改写。失败或超时安全降级为 KNOWLEDGE（原句检索）。
     */
    public GateResult judge(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new GateResult(Intent.IRRELEVANT, "");
        }
        String prompt = String.format(PROMPT_TEMPLATE, userMessage.trim());
        try {
            String response = llm.call(prompt);
            if (response != null && response.startsWith("INTENT|")) {
                String[] parts = response.split("\\|", 3);
                if (parts.length >= 2) {
                    String category = parts[1].trim().toUpperCase();
                    String query = parts.length > 2 ? parts[2].trim() : userMessage;
                    switch (category) {
                        case "CHATTING":
                            return new GateResult(Intent.CHATTING, query);
                        case "IRRELEVANT":
                            return new GateResult(Intent.IRRELEVANT, query);
                        case "KNOWLEDGE":
                        default:
                            return new GateResult(Intent.KNOWLEDGE, query.isBlank() ? userMessage : query);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("IntentGate parse failed, fallback to KNOWLEDGE: {}", e.getMessage());
        }
        // 兜底降级：作为知识问题，使用原句
        return new GateResult(Intent.KNOWLEDGE, userMessage);
    }
}
