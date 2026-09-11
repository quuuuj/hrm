package com.qiujie.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.chat.ChatProperties;
import com.qiujie.chat.entity.ChatMessage;
import com.qiujie.chat.mapper.ChatMessageMapper;
import com.qiujie.staff.vo.StaffDeptVO;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一问答会话窗口管理组件（ConversationWindow）。
 * <p>
 * 遵循 RFC #69 定案：
 * <ul>
 *   <li>零后台 LLM 调用（彻底废弃 L1/L2/L3 摘要计算与 chat_session_context 表）</li>
 *   <li>原始历史消息窗口：按字符预算（{@link ChatProperties#getWindowMaxChars()}）由近及远截断</li>
 *   <li>员工基础档案注入：姓名、工号、部门名称直接注入系统提示词</li>
 * </ul>
 * </p>
 */
@Component
public class ConversationWindow {

    private final ChatMessageMapper messageMapper;
    private final ChatProperties chatProperties;

    public ConversationWindow(ChatMessageMapper messageMapper, ChatProperties chatProperties) {
        this.messageMapper = messageMapper;
        this.chatProperties = chatProperties;
    }

    /**
     * 获取截断后的历史对话消息。
     * 从最近一条向前回溯，累加字符数不超过 windowMaxChars，保持时序升序返回。
     */
    public List<Message> loadHistory(Long sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        int maxChars = chatProperties.getWindowMaxChars();
        // 拉取最近最多 30 条消息（按时间倒序）
        List<ChatMessage> recentMessages = messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByDesc("create_time")
                        .last("LIMIT 30"));

        if (recentMessages == null || recentMessages.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> accepted = new ArrayList<>();
        int currentChars = 0;
        for (ChatMessage msg : recentMessages) {
            String content = msg.getContent() == null ? "" : msg.getContent();
            if (currentChars + content.length() > maxChars && !accepted.isEmpty()) {
                break;
            }
            accepted.add(msg);
            currentChars += content.length();
        }

        // 恢复时间升序
        Collections.reverse(accepted);

        List<Message> result = new ArrayList<>(accepted.size());
        for (ChatMessage m : accepted) {
            String role = m.getRole();
            String c = m.getContent() == null ? "" : m.getContent();
            if ("USER".equalsIgnoreCase(role)) {
                result.add(new UserMessage(c));
            } else {
                result.add(new AssistantMessage(c));
            }
        }
        return result;
    }

    /**
     * 组装系统提示词，注入员工档案信息（姓名/工号/部门）及外部知识上下文。
     *
     * @param staffInfo 员工信息（可为 null）
     * @param knowledgeContext 知识库检索片段（可为 null）
     * @return 完整的 system prompt
     */
    public String buildSystemPrompt(StaffDeptVO staffInfo, String knowledgeContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是企业智能问答助手。请基于已知知识和员工背景专业、准确、客观地回答。\n");

        if (staffInfo != null) {
            sb.append("\n【当前员工档案】\n");
            if (staffInfo.getName() != null) sb.append("姓名：").append(staffInfo.getName()).append("\n");
            if (staffInfo.getCode() != null) sb.append("工号：").append(staffInfo.getCode()).append("\n");
            if (staffInfo.getDeptName() != null) sb.append("所属部门：").append(staffInfo.getDeptName()).append("\n");
        }

        if (knowledgeContext != null && !knowledgeContext.isBlank()) {
            sb.append("\n【参考知识库内容】\n");
            sb.append(knowledgeContext).append("\n");
            sb.append("\n回答规则：\n");
            sb.append("1. 优先根据上述参考知识库内容作答。\n");
            sb.append("2. 若参考知识不足以回答，请诚实说明已知范围，切勿编造虚假制度。\n");
        }

        return sb.toString();
    }
}
