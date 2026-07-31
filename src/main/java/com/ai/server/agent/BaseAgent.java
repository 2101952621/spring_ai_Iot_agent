package com.ai.server.agent;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.model.vo.ChatEventVO;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI代理接口，定义处理聊天事件和会话的核心能力
 */
public interface BaseAgent {

    Object[] EMPTY_OBJECTS = new Object[0];

    Flux<ChatEventVO> processStream(String question, String sessionId, UUID userId);

    String process(String question, String sessionId, UUID userId);

    AgentTypeEnum getAgentType();

    void stop(String sessionId);

    default String systemMessage() {
        return "";
    }

    default Object[] tools() {
        return EMPTY_OBJECTS;
    }

    default Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of();
    }

    default List<Advisor> advisors() {
        return List.of();
    }

    default Map<String, Object> advisorParams(UUID userId, String sessionId, String requestId) {
        return Map.of();
    }

    default Map<String, Object> systemMessageParams() {
        return Map.of();
    }
}
