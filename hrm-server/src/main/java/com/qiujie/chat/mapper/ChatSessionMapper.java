package com.qiujie.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一问答会话 Mapper
 *
 * @author quuj
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
