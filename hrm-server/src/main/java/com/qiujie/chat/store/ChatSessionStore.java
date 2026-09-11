package com.qiujie.chat.store;

import com.qiujie.chat.entity.ChatSession;

import java.util.List;
import java.util.Map;

/**
 * 会话存储端口——隐藏 {@code chat_session} / {@code chat_message} 表的持久化编排。
 * <p>
 * 遵循 RFC #69：删除 mode / switchMode / context 表关联，保留会话与消息基础 CRUD。
 * 生产实现为 {@link JdbcChatSessionStore}。
 * </p>
 */
public interface ChatSessionStore {

    /**
     * 获取或创建会话。
     * sessionId 非空且存在则复用；否则新建会话，标题取消息前 50 字符。
     */
    ChatSession openOrCreate(Long sessionId, String message, Integer staffId);

    /** 按主键获取单个会话元数据。 */
    ChatSession getById(Long sessionId);

    /** 当前员工的历史会话列表，按更新时间倒序。 */
    List<ChatSession> listSessions(Integer staffId);

    /**
     * 游标分页消息历史。
     *
     * @return { records(升序), hasMore, nextCursor }
     */
    Map<String, Object> listMessages(Long sessionId, String before, int size);

    /** 删除会话——级联删除其下的所有消息。 */
    void delete(Long sessionId);
}
