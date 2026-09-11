package com.qiujie.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 统一问答请求体。
 * <p>mode/scene 字段随模式体系删除——意图由服务端 IntentGate 自动判定。</p>
 */
public class ChatRequest {

    private Long sessionId;
    private String message;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    /** 兼容前端旧字段名 conversationId。 */
    @JsonProperty("conversationId")
    public void setConversationId(Long conversationId) { this.sessionId = conversationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
