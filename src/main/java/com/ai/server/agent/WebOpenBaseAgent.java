package com.ai.server.agent;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.agent.tools.WebFunctionTools;
import com.ai.server.config.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 网页功能打开智能体
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebOpenBaseAgent extends AbstractBaseAgent {

    private final WebFunctionTools webFunctionTools;

    @Override
    public String systemMessage() {
        return SystemConstant.WEB_OPEN;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.WEB_OPEN;
    }

    @Override
    public Object[] tools() {
        return new Object[]{webFunctionTools};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of("requestId", requestId);
    }

    @Override
    protected void beforeProcessStream(String question, String sessionId, UUID userId, String requestId) {
        log.info("[WEB OPEN] 收到请求: sessionId={}, question={}", sessionId, question);
    }

    @Override
    protected void afterProcessStream(String sessionId, UUID userId, String finalContent) {
        log.info("[WEB OPEN] 响应完成: sessionId={}, responseLength={}", sessionId,
                finalContent == null ? 0 : finalContent.length());
    }
}