package com.ai.server.agent.orchestrator;

import com.ai.server.agent.BaseAgent;
import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.agent.enums.UserChatEventType;
import com.ai.server.model.vo.ChatEventVO;
import com.ai.server.service.ai.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 智能体中央编排器 —  Orchestrator + Strategy Pattern
 * <pre>
 * 执行流程：
 *   1. 会话更新（统一入口，避免重复调用）
 *   2. 意图分类（RouteAgent 同步调用）
 *   3. 策略分发（根据意图结果匹配具体 Agent）
 *   4. 流式执行 + 兜底处理
 * </pre>
 */
@Slf4j
@Component
public class AgentOrchestrator {

    /**
     * Agent 注册表：AgentTypeEnum → BaseAgent 的映射
     */
    private final Map<AgentTypeEnum, BaseAgent> agentRegistry;
    private final BaseAgent routeAgent;
    private final AgentService agentService;
    private final ChatMemory chatMemory;

    public AgentOrchestrator(List<BaseAgent> agents,
                             @Lazy AgentService agentService,
                             ChatMemory chatMemory) {
        this.agentRegistry = agents.stream()
                .collect(Collectors.toMap(
                        BaseAgent::getAgentType,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("检测到重复的 AgentTypeEnum: {}, 保留已注册的 Agent", existing.getAgentType());
                            return existing;
                        },
                        ConcurrentHashMap::new
                ));
        this.routeAgent = agentRegistry.get(AgentTypeEnum.ROUTE);
        this.agentService = agentService;
        this.chatMemory = chatMemory;
        log.info("AgentOrchestrator 初始化完成，已注册 {} 个智能体: {}",
                agentRegistry.size(),
                agentRegistry.keySet().stream().map(AgentTypeEnum::getAgentName).toList());
    }

    /**
     * 编排核心入口：意图识别 → 策略分发 → 流式响应
     *
     * @param question  用户问题
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 流式聊天事件
     */
    public Flux<ChatEventVO> orchestrate(String question, String sessionId, UUID userId) {
        agentService.update(sessionId, question, userId);
        // 立即持久化用户消息到 ChatMemory，防止路由/分发阶段断连导致消息丢失
        String conversationId = agentService.getConversationId(userId, sessionId);
        chatMemory.add(conversationId, new UserMessage(question));
        log.debug("用户消息已预保存: sessionId={}, conversationId={}", sessionId, conversationId);

        if (routeAgent == null) {
            log.error("路由智能体 (ROUTE) 未注册，无法进行意图识别");
            return fallbackResponse("系统路由服务暂不可用，请稍后再试");
        }
        String intentResult;
        try {
            intentResult = routeAgent.process(question, sessionId, userId);
            log.info("意图识别结果: sessionId={}, result={}", sessionId, intentResult);
        } catch (Exception e) {
            log.error("意图识别失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return fallbackResponse("服务器暂时无响应，请稍后再试！");
        }
        AgentTypeEnum targetType = AgentTypeEnum.agentNameOf(intentResult);
        if (targetType != null) {
            BaseAgent targetAgent = agentRegistry.get(targetType);
            if (targetAgent != null) {
                log.info("意图分发: sessionId={}, targetAgent={}", sessionId, targetType);
                return targetAgent.processStream(question, sessionId, userId);
            }
            log.warn("意图类型 {} 已识别但未注册对应的 Agent, sessionId={}", targetType, sessionId);
        }
        log.info("兜底响应: sessionId={}, intentResult={}", sessionId, intentResult);
        return Flux.just(
                ChatEventVO.builder()
                        .eventType(UserChatEventType.DATA.getValue())
                        .eventData(Objects.requireNonNullElse(intentResult, "抱歉，我暂时无法处理这个请求"))
                        .build(),
                ChatEventVO.builder()
                        .eventType(UserChatEventType.STOP.getValue())
                        .build()
        );
    }

    /**
     * 停止指定会话的所有智能体任务
     */
    public void stop(String sessionId) {
        agentRegistry.values().forEach(agent -> {
            try {
                agent.stop(sessionId);
            } catch (Exception e) {
                log.warn("停止 Agent [{}] 时发生异常: sessionId={}, error={}",
                        agent.getAgentType(), sessionId, e.getMessage());
            }
        });
    }

    /**
     * 获取注册的智能体数量
     */
    public int getRegisteredAgentCount() {
        return agentRegistry.size();
    }

    /**
     * 检查是否已注册指定类型的智能体
     */
    public boolean isAgentRegistered(AgentTypeEnum type) {
        return agentRegistry.containsKey(type);
    }

    /**
     * 兜底响应流
     */
    private Flux<ChatEventVO> fallbackResponse(String message) {
        return Flux.just(
                ChatEventVO.builder()
                        .eventType(UserChatEventType.DATA.getValue())
                        .eventData(message)
                        .build(),
                ChatEventVO.builder()
                        .eventType(UserChatEventType.STOP.getValue())
                        .build()
        );
    }
}
