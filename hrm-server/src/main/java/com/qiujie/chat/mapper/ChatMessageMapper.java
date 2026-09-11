package com.qiujie.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一问答消息 Mapper
 *
 * @author quuj
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(Long sessionId);
}
