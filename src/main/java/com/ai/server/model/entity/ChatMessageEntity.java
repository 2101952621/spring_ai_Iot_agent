package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;

import java.io.Serializable;
import java.util.Date;

/**
 * 对话消息实体
 */
@Data
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 128)
    private String conversationId;

    @Column(name = "message_index", nullable = false)
    private Integer messageIndex;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @PrePersist
    protected void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
