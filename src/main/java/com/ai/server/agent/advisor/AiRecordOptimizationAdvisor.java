package com.ai.server.agent.advisor;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.agent.memory.DBChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * 记录优化 Advisor — 在 AI 响应返回后，根据输出内容自动优化会话记录
 */
@Slf4j
public class AiRecordOptimizationAdvisor implements BaseAdvisor {

    private final DBChatMemoryRepository chatMemoryRepository;

    public AiRecordOptimizationAdvisor(DBChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        try {
            doOptimization(chatClientResponse);
        } catch (Exception e) {
            log.error("记录优化异常，已忽略: {}", e.getMessage(), e);
        }
        return chatClientResponse;
    }

    private void doOptimization(ChatClientResponse chatClientResponse) {
        var chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            return;
        }
        var result = chatResponse.getResult();
        var text = result.getOutput().getText();
        var agentType = AgentTypeEnum.agentNameOf(text);
        if (agentType == null) {
            return;
        }
        var context = chatClientResponse.context();
        var conversationId = (String) context.get(ChatMemory.CONVERSATION_ID);
        if (conversationId == null) {
            log.warn("conversationId 为空，跳过优化");
            return;
        }
        this.chatMemoryRepository.optimizationRecord(conversationId, agentType);
        log.debug("记录优化完成: conversationId={}, agentType={}", conversationId, agentType);
    }

    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100;
    }
}
