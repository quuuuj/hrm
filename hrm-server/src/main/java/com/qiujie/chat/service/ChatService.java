package com.qiujie.chat.service;

import com.qiujie.chat.dto.ChatRequest;
import com.qiujie.chat.entity.ChatSession;
import com.qiujie.chat.store.ChatSessionStore;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.security.ServiceException;
import com.qiujie.staff.service.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public static final int MAX_MESSAGE_LENGTH = 1000;

    @Autowired
    private ChatSessionStore sessionStore;
    @Autowired
    private ChatQaService chatQaService;
    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 流式对话入口。
     * <p>不加 {@code @Transactional}：会话行必须在异步问答任务开始前已提交，
     * 否则任务里的 {@code selectById} 读不到新会话，{@code message_count} 与时间戳都不会更新。</p>
     */
    public SseEmitter chat(ChatRequest request) {
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            throw new ServiceException(BusinessStatusEnum.ERROR.getCode(), "消息不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new ServiceException(BusinessStatusEnum.ERROR.getCode(),
                    "消息过长，请控制在 " + MAX_MESSAGE_LENGTH + " 字以内");
        }
        Integer staffId = currentStaffId();
        // 1. 获取或创建会话
        ChatSession session = sessionStore.openOrCreate(
                request.getSessionId(), message, staffId);

        // 2. 委托统一问答管线执行
        return chatQaService.streamAnswer(session.getId(), message);
    }

    // ==================== 查询（会话管理） ====================

    public List<ChatSession> listSessions() {
        return sessionStore.listSessions(currentStaffId());
    }

    public ChatSession getSession(Long sessionId) {
        ChatSession session = sessionStore.getById(sessionId);
        return owns(session) ? session : null;
    }

    public Map<String, Object> listMessages(Long sessionId, Long before, int size) {
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
