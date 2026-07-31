package com.ai.server.agent.memory;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.model.entity.ChatMessageEntity;
import com.ai.server.service.ai.ChatMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 PostgreSQL 的 ChatMemoryRepository 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DBChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;


    @Override
    public List<String> findConversationIds() {
        return chatMessageService.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        return chatMessageService.findByConversationId(conversationId)
                .stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");
        List<ChatMessageEntity> entities = messages.stream()
                .map(message -> toEntity(conversationId, messages.indexOf(message), message))
                .collect(Collectors.toList());
        chatMessageService.saveAll(conversationId, entities);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        chatMessageService.deleteByConversationId(conversationId);
    }

    private Message toMessage(ChatMessageEntity entity) {
        String type = entity.getMessageType();
        String content = entity.getMessageContent();
        return switch (type) {
            case "USER" -> new UserMessage(content);
            case "SYSTEM" -> new SystemMessage(content);
            default -> new AssistantMessage(content);
        };
    }

    private ChatMessageEntity toEntity(String conversationId, int index, Message message) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setConversationId(conversationId);
        entity.setMessageIndex(index);
        entity.setMessageType(message.getMessageType().name());
        entity.setMessageContent(message.getText());
        entity.setMetadata(toJson(message.getMetadata()));
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("解析消息 metadata 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("序列化消息 metadata 失败: {}", e.getMessage());
            return null;
        }
    }

    public void optimizationRecord(String conversationId, AgentTypeEnum agentType) {
        chatMessageService.optimization(conversationId, agentType.getAgentName());
    }
}
