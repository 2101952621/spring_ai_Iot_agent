package com.ai.server.service.ai;

import com.ai.server.agent.orchestrator.AgentOrchestrator;
import com.ai.server.model.vo.ChatEventVO;
import com.ai.server.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Agent 服务实现 — 会话管理 + 委托 Orchestrator 编排
 *
 * 职责分离：
 * - AgentOrchestrator：意图识别 → 策略分发 → 流式执行
 * - AgentServiceImpl：  会话生命周期管理（创建、更新、查询）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentOrchestrator agentOrchestrator;
    private final ChatSessionService chatSessionService;

    @Override
    public String getConversationId(UUID userId, String sessionId) {
        return userId.toString() + "_" + sessionId;
    }

    @Override
    public void update(String sessionId, String question, UUID userId) {
        chatSessionService.update(sessionId, question, userId);
    }

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId, SecurityUser currentUser) {
        return agentOrchestrator.orchestrate(question, sessionId, currentUser.getUuidId());
    }

    @Override
    public void chatStop(String sessionId) {
        agentOrchestrator.stop(sessionId);
    }
}
