package com.qiujie.chat.store;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.chat.entity.ChatMessage;
import com.qiujie.chat.entity.ChatSession;
import com.qiujie.chat.mapper.ChatMessageMapper;
import com.qiujie.chat.mapper.ChatSessionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 会话存储 JDBC 适配器——{@link ChatSessionStore} 的生产实现。
 */
@Component
public class JdbcChatSessionStore implements ChatSessionStore {

    private static final int MAX_PAGE_SIZE = 50;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    public JdbcChatSessionStore(ChatSessionMapper sessionMapper,
                                ChatMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public ChatSession openOrCreate(Long sessionId, String message, Integer staffId) {
        if (sessionId != null) {
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && Objects.equals(session.getStaffId(), staffId)) return session;
        }

        ChatSession session = new ChatSession()
                .setStaffId(staffId)
                .setTitle(message != null && !message.isBlank()
                        ? message.substring(0, Math.min(50, message.length()))
                        : "新会话")
                .setStatus("ACTIVE")
                .setMessageCount(0)
                .setLastMessageAt(LocalDateTime.now())
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        return session;
    }

    @Override
    public ChatSession getById(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public List<ChatSession> listSessions(Integer staffId) {
        return sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)
                        .orderByDesc("update_time"));
    }

    @Override
    public Map<String, Object> listMessages(Long sessionId, String before, int size) {
        int limit = Math.min(size, MAX_PAGE_SIZE);
        var qw = new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId);
        if (before != null) {
            qw.lt("create_time", before);
        }
        qw.orderByDesc("create_time").last("LIMIT " + (limit + 1));

        List<ChatMessage> desc = messageMapper.selectList(qw);
        boolean hasMore = desc.size() > limit;
        if (hasMore) desc = desc.subList(0, limit);
        Collections.reverse(desc);

        String nextCursor = null;
        if (!desc.isEmpty() && desc.get(0).getCreateTime() != null) {
            nextCursor = desc.get(0).getCreateTime().toString();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", desc);
        result.put("hasMore", hasMore);
        result.put("nextCursor", nextCursor);
        return result;
    }

    @Override
    @Transactional
    public void delete(Long sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);
    }
}
