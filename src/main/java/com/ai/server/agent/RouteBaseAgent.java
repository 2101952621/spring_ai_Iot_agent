package com.ai.server.agent;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.config.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 意图路由智能体 — AgentOrchestrator 中的 Intent Classifier
 * <pre>
 * 职责：
 *   1. 接收用户原始输入
 *   2. 通过 LLM + System Prompt 识别意图
 *   3. 返回意图编码（RECOMMEND / KNOWLEDGE / 兜底文本）
 *
 * 注意：
 *   - 此 Agent 的 process() 方法由 AgentOrchestrator 调用，用于意图分类
 *   - processStream() 一般不会在此 Agent 上调用（路由Agent本身不提供流式业务响应）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteBaseAgent extends AbstractBaseAgent {

    @Autowired
    @Qualifier("routeChatClient")
    private ChatClient routeChatClient;

    @Override
    public String systemMessage() {
        return SystemConstant.ROUTER;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    @Override
    protected ChatClient.ChatClientRequestSpec getChatClientRequest(
            UUID userId, String sessionId, String requestId,
            String conversationId, String question) {
        return routeChatClient.prompt()
                .system(promptSystem ->
                        promptSystem.text(systemMessage()).params(systemMessageParams()))
                .user(question);
    }

    /**
     * 意图分类前置处理 — 记录分类请求
     */
    @Override
    protected void beforeProcessStream(String question, String sessionId, java.util.UUID userId, String requestId) {
        log.debug("RouteBaseAgent 开始意图分类: sessionId={}, question={}", sessionId, question);
    }

    /**
     * 意图分类后置处理 — 记录分类结果
     */
    @Override
    protected void afterProcessStream(String sessionId, java.util.UUID userId, String finalContent) {
        log.debug("RouteBaseAgent 意图分类完成: sessionId={}, result={}", sessionId, finalContent);
    }
}
