package com.qiujie.chat.service;

import com.qiujie.chat.dto.ChatRequest;
import com.qiujie.chat.entity.ChatSession;
import com.qiujie.chat.store.ChatSessionStore;
import com.qiujie.staff.service.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 统一智能问答生命周期服务。
 * <p>
 * 职责：负责会话 CRUD + 委托 {@link ChatQaService} 执行问答管线。
 * </p>
 */
@Service
public class ChatService {

    private final ChatSessionStore sessionStore;
    private final ChatQaService chatQaService;
    private final SecurityUtil securityUtil;

    public ChatService(ChatSessionStore sessionStore,
                       ChatQaService chatQaService,
                       SecurityUtil securityUtil) {
        this.sessionStore = sessionStore;
        this.chatQaService = chatQaService;
        this.securityUtil = securityUtil;
    }

    /**
     * 流式对话入口。
     */
    @Transactional
    public SseEmitter chat(ChatRequest request) {
        Integer staffId = currentStaffId();
        // 1. 获取或创建会话
        ChatSession session = sessionStore.openOrCreate(
                request.getSessionId(), request.getMessage(), staffId);

        // 2. 委托统一问答管线执行
        return chatQaService.streamAnswer(session.getId(), request.getMessage());
    }

    // ==================== 查询（会话管理） ====================

    public List<ChatSession> listSessions() {
        return sessionStore.listSessions(currentStaffId());
    }

    public ChatSession getSession(Long sessionId) {
        ChatSession session = sessionStore.getById(sessionId);
        return owns(session) ? session : null;
    }

    public Map<String, Object> listMessages(Long sessionId, String before, int size) {
        if (!owns(sessionStore.getById(sessionId))) {
            Map<String, Object> empty = new java.util.HashMap<>();
            empty.put("records", List.of());
            empty.put("hasMore", false);
            empty.put("nextCursor", null);
            return empty;
        }
        return sessionStore.listMessages(sessionId, before, size);
    }

    public void deleteSession(Long sessionId) {
        if (owns(sessionStore.getById(sessionId))) {
            sessionStore.delete(sessionId);
        }
    }

    private boolean owns(ChatSession session) {
        return session != null && java.util.Objects.equals(session.getStaffId(), currentStaffId());
    }

    /** 当前登录员工 ID（JWT），供会话查询范围限定。 */
    public Integer currentStaffId() {
        return securityUtil.getCurrentOperatorId();
    }
}
