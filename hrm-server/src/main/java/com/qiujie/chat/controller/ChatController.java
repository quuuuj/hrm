package com.qiujie.chat.controller;

import com.qiujie.chat.dto.ChatRequest;
import com.qiujie.chat.entity.ChatSession;
import com.qiujie.chat.service.ChatService;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 统一智能问答控制器。
 * <p>
 * 端点统一映射至 {@code /chat/*}。
 * 遵循 RFC #69：删除 mode / switchMode 接口。
 * </p>
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * SSE 流式对话接口。
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * 获取当前员工的所有历史会话列表。
     */
    @GetMapping("/conversations")
    public ResponseDTO listSessions() {
        List<ChatSession> sessions = chatService.listSessions();
        return Response.success(sessions);
    }

    /**
     * 获取单个会话元数据。
     */
    @GetMapping("/conversations/{id}")
    public ResponseDTO getSession(@PathVariable Long id) {
        ChatSession session = chatService.getSession(id);
        if (session == null) {
            return Response.error("会话不存在");
        }
        return Response.success(session);
    }

    /**
     * 游标分页获取会话历史消息。
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseDTO listMessages(@PathVariable Long id,
                                    @RequestParam(defaultValue = "15") int size,
                                    @RequestParam(required = false) String before) {
        return Response.success(chatService.listMessages(id, before, size));
    }

    /**
     * 删除会话及其历史消息。
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseDTO deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return Response.success();
    }
}
