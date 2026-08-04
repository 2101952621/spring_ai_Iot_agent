package com.ai.server.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史消息搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSearchVO {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话标题（用于列表展示）
     */
    private String sessionTitle;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息内容（原始）
     */
    private String messageContent;

    /**
     * 高亮片段（带 <em> 标签），前端可直接渲染
     */
    private String highlight;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * ES 相关性分数
     */
    private Float score;
}