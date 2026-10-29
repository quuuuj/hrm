package com.qiujie.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.chat.ChatProperties;
import com.qiujie.chat.entity.ChatMessage;
import com.qiujie.chat.entity.ChatSession;
import com.qiujie.chat.llm.ChatLlm;
import com.qiujie.chat.mapper.ChatMessageMapper;
import com.qiujie.chat.mapper.ChatSessionMapper;
import com.qiujie.chat.service.IntentGate.GateResult;
import com.qiujie.staff.mapper.StaffMapper;
import com.qiujie.staff.service.SecurityUtil;
import com.qiujie.staff.vo.StaffDeptVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 统一智能问答服务核心编排（ChatQaService）。
 * <p>
 * 遵循 RFC #69 统一问答架构：
 * <ol>
 *   <li>意图闸门（IntentGate）单次调用分类 + 改写</li>
 *   <li>IRRELEVANT → 固定文案直接推送，零模型开销</li>
 *   <li>CHATTING → 注入员工档案 + 历史窗口，直接模型回答，不检索知识库</li>
 *   <li>KNOWLEDGE → 检索知识库 + 证据评估 → 注入知识上下文与档案 → 模型回答</li>
 *   <li>落库分两步：问题先写（与回答是否成功无关），回答在流结束后写</li>
 * </ol>
 * </p>
 */
@Service
public class ChatQaService {

    private static final Logger log = LoggerFactory.getLogger(ChatQaService.class);
    private static final long SSE_TIMEOUT = 300_000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String IRRELEVANT_REPLY =
            "抱歉，我是企业内部问答助手，仅能协助解答公司规章制度、工作流程与日常咨询。请提出与工作相关的问题。";
    private static final String CHATTING_FALLBACK =
            "您好！很高兴为您服务，请问有什么关于公司规章制度或日常工作可以协助您的？";
    private static final String KNOWLEDGE_FALLBACK =
            "抱歉，根据公司知识库暂未找到足够的相关信息以准确回答该问题，建议咨询相关业务负责人或 HR。";

    @Autowired
    private IntentGate intentGate;
    @Autowired
    private ConversationWindow conversationWindow;
    @Autowired
    private ChatLlm chatLlm;
    @Autowired
    private HybridRetrievalService searchProvider;
    @Autowired
    private EvidenceAssessmentService evidenceService;
    @Autowired
    private QaRecordRepository qaRecordRepository;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private SecurityUtil securityUtil;
    @Autowired
    private ChatProperties chatProperties;
    @Autowired
    @Qualifier("chatExecutor")
    private ThreadPoolTaskExecutor chatExecutor;

    /**
     * 核心问答接口，返回 SSE 流式结果。
     */
    public SseEmitter streamAnswer(Long sessionId, String userQuestion) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer staffId = securityUtil.getCurrentOperatorId();

        try {
            chatExecutor.execute(() -> {
                SecurityContextHolder.getContext().setAuthentication(auth);
                try {
                    answer(sessionId, userQuestion, staffId, emitter);
                } catch (Exception e) {
                    log.error("ChatQaService streamAnswer failed for session: {}", sessionId, e);
                    fail(emitter, "抱歉，问答服务异常，请稍后重试");
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        } catch (Exception rejected) {
            // 队列已满：不阻塞 HTTP 线程，直接把错误推给前端
            log.warn("Chat executor rejected task for session {}", sessionId);
            fail(emitter, "当前咨询人数较多，请稍后重试");
        }

        return emitter;
    }

    /**
     * 问答管线主体（包内可见以便单测直接驱动，不经线程池与 SSE 生命周期）。
     */
    void answer(Long sessionId, String userQuestion, Integer staffId, SseEmitter emitter) throws Exception {
        // 1. 员工档案（用于注入系统提示词）
        StaffDeptVO staffInfo = currentStaff(userQuestion, staffId);

        // 问题先落库：意图判定/模型调用都可能抛异常，用户说过的话不该因此丢失
        saveUserMessage(sessionId, userQuestion);

        // 2. 意图分类与改写
        GateResult gateResult = intentGate.judge(userQuestion);

        // 分支 A: 无关/恶意输入，固定文案，零大模型调用
        if (gateResult.intent() == IntentGate.Intent.IRRELEVANT) {
            finish(emitter, sessionId, userQuestion, IRRELEVANT_REPLY, staffId, null);
            return;
        }

        // 3. 准备历史消息窗口
        List<Message> history = conversationWindow.loadHistory(sessionId);

        // 分支 B: 闲聊意图，不检索，直接作答
        if (gateResult.intent() == IntentGate.Intent.CHATTING) {
            String systemPrompt = conversationWindow.buildSystemPrompt(staffInfo, null);
            String answer = stream(sessionId, userQuestion, history, systemPrompt, emitter);
            if (answer == null) {
                answer = CHATTING_FALLBACK;
                push(emitter, answer);
            }
            finish(emitter, sessionId, userQuestion, answer, staffId, null);
            return;
        }

        // 分支 C: 知识库问答
        String queryToSearch = (gateResult.rewrittenQuery() != null && !gateResult.rewrittenQuery().isBlank())
                ? gateResult.rewrittenQuery()
                : userQuestion;

        List<SearchResult> searchResults = searchProvider.search(List.of(queryToSearch));
        EvidenceAssessmentService.Assessment assessment = evidenceService.assess(searchResults);

        String kbContext = buildKbContext(searchResults);
        String systemPrompt = conversationWindow.buildSystemPrompt(staffInfo, kbContext);
        String answer = stream(sessionId, userQuestion, history, systemPrompt, emitter);
        if (answer == null) {
            answer = KNOWLEDGE_FALLBACK;
            push(emitter, answer);
        }
        finish(emitter, sessionId, userQuestion, answer, staffId, assessment);
    }

    /** 拉取当前员工档案；取不到时返回 null，系统提示词跳过档案段落。 */
    private StaffDeptVO currentStaff(String userQuestion, Integer staffId) {
        if (staffId == null) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String code = auth == null ? null : auth.getName();
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return staffMapper.queryByCode(code);
        } catch (Exception e) {
            log.warn("Load staff profile failed for question [{}]: {}", userQuestion, e.getMessage());
            return null;
        }
    }

    /** 按字符预算拼装检索上下文。 */
    private String buildKbContext(List<SearchResult> searchResults) {
        StringBuilder kbContext = new StringBuilder();
        int maxContext = chatProperties.getMaxContextChars();
        int currentLen = 0;
        for (SearchResult sr : searchResults) {
            String chunk = sr.chunkText();
            if (chunk == null) {
                continue;
            }
            if (currentLen + chunk.length() > maxContext && currentLen > 0) {
                break;
            }
            kbContext.append("【来源: ").append(sr.documentName()).append("】\n")
                    .append(chunk).append("\n\n");
            currentLen += chunk.length();
        }
        return kbContext.toString();
    }

    /**
     * 真流式回答：每个模型增量立刻推给前端。
     *
     * @return 完整回答；模型无输出或失败返回 null
     */
    private String stream(Long sessionId, String userQuestion, List<Message> history,
                          String systemPrompt, SseEmitter emitter) {
        try {
            return chatLlm.chatStream(history, userQuestion, systemPrompt, delta -> push(emitter, delta));
        } catch (Exception e) {
            log.warn("Stream answer failed for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /** 推送完成事件并落库回答。 */
    private void finish(SseEmitter emitter, Long sessionId, String userQuestion, String answer,
                        Integer staffId, EvidenceAssessmentService.Assessment assessment) throws Exception {
        saveAssistantMessage(sessionId, answer);
        qaRecordRepository.save(userQuestion, answer,
                assessment == null ? null : assessment.level().name());        emitter.send(SseEmitter.event().name("meta")
                .data(MAPPER.writeValueAsString(Map.of("conversationId", sessionId))));
        emitter.complete();
    }

    /** 推 error 事件并结束连接（不用 completeWithError，避免错误分发改写成 200）。 */
    private void fail(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
        } catch (Exception ignored) {
            emitter.completeWithError(new IllegalStateException(message));
        }
    }

    /** 推送一段文本；连接已断开时抛异常中断生成，避免继续白烧模型调用。 */
    private void push(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("token").data(text));
        } catch (Exception e) {
            throw new IllegalStateException("SSE 连接已断开", e);
        }
    }

    private void saveUserMessage(Long sessionId, String question) {
        if (sessionId == null) {
            return;
        }
        insertMessage(sessionId, "USER", question);
    }

    private void saveAssistantMessage(Long sessionId, String answer) {
        if (sessionId == null) {
            return;
        }
        insertMessage(sessionId, "ASSISTANT", answer);
    }

    private void insertMessage(Long sessionId, String role, String content) {
        LocalDateTime now = LocalDateTime.now();
        messageMapper.insert(new ChatMessage()
                .setSessionId(sessionId)
                .setRole(role)
                .setContent(content)
                .setCreateTime(now));

        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            int currentCount = session.getMessageCount() != null ? session.getMessageCount() : 0;
            session.setMessageCount(currentCount + 1);
            session.setLastMessageAt(now);
            session.setUpdateTime(now);
            sessionMapper.updateById(session);
        }
    }
}
