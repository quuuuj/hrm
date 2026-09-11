package com.qiujie.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.chat.ChatProperties;
import com.qiujie.chat.entity.ChatMessage;
import com.qiujie.chat.entity.ChatSession;
import com.qiujie.chat.llm.ChatLlm;
import com.qiujie.chat.mapper.ChatMessageMapper;
import com.qiujie.chat.mapper.ChatSessionMapper;
import com.qiujie.chat.service.IntentGate.GateResult;
import com.qiujie.chat.service.KnowledgeSearchProvider.SearchResult;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.SecurityUtil;
import com.qiujie.vo.StaffDeptVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
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
 *   <li>KNOWLEDGE → 检索知识库 + 证据评估 → 注入知识上下文与档案 → 模型回答，附带 citations 事件推送</li>
 *   <li>问答持久化：保存会话消息、更新会话时间、持久化 QA 评估记录</li>
 * </ol>
 * </p>
 */
@Service
public class ChatQaService {

    private static final Logger log = LoggerFactory.getLogger(ChatQaService.class);
    private static final long SSE_TIMEOUT = 300_000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IntentGate intentGate;
    private final ConversationWindow conversationWindow;
    private final ChatLlm chatLlm;
    private final KnowledgeSearchProvider searchProvider;
    private final EvidenceAssessmentService evidenceService;
    private final QaRecordRepository qaRecordRepository;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final StaffMapper staffMapper;
    private final SecurityUtil securityUtil;
    private final ChatProperties chatProperties;
    private final ThreadPoolTaskExecutor fileTaskExecutor;

    public ChatQaService(IntentGate intentGate,
                         ConversationWindow conversationWindow,
                         ChatLlm chatLlm,
                         KnowledgeSearchProvider searchProvider,
                         EvidenceAssessmentService evidenceService,
                         QaRecordRepository qaRecordRepository,
                         ChatMessageMapper messageMapper,
                         ChatSessionMapper sessionMapper,
                         StaffMapper staffMapper,
                         SecurityUtil securityUtil,
                         ChatProperties chatProperties,
                         ThreadPoolTaskExecutor fileTaskExecutor) {
        this.intentGate = intentGate;
        this.conversationWindow = conversationWindow;
        this.chatLlm = chatLlm;
        this.searchProvider = searchProvider;
        this.evidenceService = evidenceService;
        this.qaRecordRepository = qaRecordRepository;
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.staffMapper = staffMapper;
        this.securityUtil = securityUtil;
        this.chatProperties = chatProperties;
        this.fileTaskExecutor = fileTaskExecutor;
    }

    /**
     * 核心问答接口，返回 SSE 流式结果。
     */
    public SseEmitter streamAnswer(Long sessionId, String userQuestion) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer staffId = securityUtil.getCurrentOperatorId();

        fileTaskExecutor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                // 1. 获取员工信息
                StaffDeptVO staffInfo = null;
                if (auth != null && auth.getName() != null) {
                    staffInfo = staffMapper.queryByCode(auth.getName());
                }

                // 2. 意图分类与改写
                GateResult gateResult = intentGate.judge(userQuestion);

                // 分支 A: 无关/恶意输入，固定文案，零大模型调用
                if (gateResult.intent() == IntentGate.Intent.IRRELEVANT) {
                    String refuseMsg = "抱歉，我是企业内部问答助手，仅能协助解答公司规章制度、工作流程与日常咨询。请提出与工作相关的问题。";
                    pushTokens(emitter, refuseMsg);
                    saveInteraction(sessionId, userQuestion, refuseMsg, staffId);
                    completeSse(emitter, sessionId, List.of());
                    return;
                }

                // 3. 准备历史消息窗口
                List<Message> history = conversationWindow.loadHistory(sessionId);

                // 分支 B: 闲聊意图，不检索，直接作答
                if (gateResult.intent() == IntentGate.Intent.CHATTING) {
                    String systemPrompt = conversationWindow.buildSystemPrompt(staffInfo, null);
                    String answer = chatLlm.chat(history, userQuestion, systemPrompt);
                    if (answer == null || answer.isBlank()) {
                        answer = "您好！很高兴为您服务，请问有什么关于公司规章制度或日常工作可以协助您的？";
                    }
                    pushTokens(emitter, answer);
                    saveInteraction(sessionId, userQuestion, answer, staffId);
                    completeSse(emitter, sessionId, List.of());
                    return;
                }

                // 分支 C: 知识库问答
                String queryToSearch = (gateResult.rewrittenQuery() != null && !gateResult.rewrittenQuery().isBlank())
                        ? gateResult.rewrittenQuery()
                        : userQuestion;

                List<SearchResult> searchResults = searchProvider.search(List.of(queryToSearch));
                EvidenceAssessmentService.Assessment assessment = evidenceService.assess(searchResults);

                // 组装参考知识上下文
                StringBuilder kbContext = new StringBuilder();
                List<Map<String, Object>> citations = new ArrayList<>();
                int maxContext = chatProperties.getMaxContextChars();
                int currentLen = 0;

                for (SearchResult sr : searchResults) {
                    String chunk = sr.chunkText();
                    if (currentLen + chunk.length() > maxContext && currentLen > 0) {
                        break;
                    }
                    kbContext.append("【来源: ").append(sr.documentName()).append("】\n")
                            .append(chunk).append("\n\n");
                    currentLen += chunk.length();

                    citations.add(Map.of(
                            "documentId", sr.documentId() != null ? sr.documentId() : 0,
                            "documentName", sr.documentName() != null ? sr.documentName() : "",
                            "chunkId", sr.chunkId() != null ? sr.chunkId() : 0,
                            "score", sr.score()
                    ));
                }

                String systemPrompt = conversationWindow.buildSystemPrompt(staffInfo, kbContext.toString());
                String answer = chatLlm.chat(history, userQuestion, systemPrompt);
                if (answer == null || answer.isBlank()) {
                    answer = "抱歉，根据公司知识库暂未找到足够的相关信息以准确回答该问题，建议咨询相关业务负责人或 HR。";
                }

                // 推送回答与 citations
                pushTokens(emitter, answer);
                saveInteraction(sessionId, userQuestion, answer, staffId);

                // 持久化 QA 记录 (PostgreSQL/kb 库，失败静默)
                qaRecordRepository.save(userQuestion, answer, assessment.level().name(), citations.size());

                completeSse(emitter, sessionId, citations);

            } catch (Exception e) {
                log.error("ChatQaService streamAnswer failed for session: {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("抱歉，问答服务异常，请稍后重试"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    private void pushTokens(SseEmitter emitter, String answer) throws Exception {
        for (int i = 0; i < answer.length(); i++) {
            String ch = answer.substring(i, i + 1);
            emitter.send(SseEmitter.event().name("token").data(ch));
            if (i % 5 == 0) {
                Thread.sleep(5);
            }
        }
    }

    private void completeSse(SseEmitter emitter, Long sessionId, List<Map<String, Object>> citations) throws Exception {
        if (!citations.isEmpty()) {
            emitter.send(SseEmitter.event().name("citations").data(MAPPER.writeValueAsString(citations)));
        }
        emitter.send(SseEmitter.event().name("meta").data(MAPPER.writeValueAsString(Map.of("conversationId", sessionId))));
        emitter.complete();
    }

    private void saveInteraction(Long sessionId, String question, String answer, Integer staffId) {
        if (sessionId == null) return;
        LocalDateTime now = LocalDateTime.now();

        // 1. 保存用户问题
        ChatMessage userMsg = new ChatMessage()
                .setSessionId(sessionId)
                .setRole("USER")
                .setContent(question)
                .setCreateTime(now);
        messageMapper.insert(userMsg);

        // 2. 保存助手回答
        ChatMessage botMsg = new ChatMessage()
                .setSessionId(sessionId)
                .setRole("ASSISTANT")
                .setContent(answer)
                .setCreateTime(now);
        messageMapper.insert(botMsg);

        // 3. 更新会话状态与统计
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            int currentCount = session.getMessageCount() != null ? session.getMessageCount() : 0;
            session.setMessageCount(currentCount + 2);
            session.setLastMessageAt(now);
            session.setUpdateTime(now);
            sessionMapper.updateById(session);
        }
    }
}
