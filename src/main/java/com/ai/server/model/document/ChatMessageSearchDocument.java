package com.ai.server.model.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息 ES 索引文档
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSearchDocument {

    /** 消息ID（PG chat_message.id） */
    private String id;

    /** 会话ID（PG chat_session.session_id） */
    private String sessionId;

    /** 完整 conversationId（userId_sessionId） */
    private String conversationId;

    /** 用户ID（UUID 字符串） */
    private String userId;

    /** 消息类型：USER / ASSISTANT / SYSTEM / TOOL */
    private String messageType;

    /** 消息内容（用于分词与匹配） */
    private String messageContent;

    /** 消息在会话内的序号 */
    private Integer messageIndex;

    /** 会话标题（冗余存储，避免联表） */
    private String sessionTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}