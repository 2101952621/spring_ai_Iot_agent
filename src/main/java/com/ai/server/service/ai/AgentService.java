package com.ai.server.service.ai;

import com.ai.server.model.vo.ChatEventVO;
import com.ai.server.security.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AI代理服务接口
 */
public interface AgentService {

    String getConversationId(UUID userId, String sessionId);

    void update(String sessionId, String question, UUID userId);

    Flux<ChatEventVO> chat(String question, String sessionId, SecurityUser currentUser);

    void chatStop(String sessionId);
}
