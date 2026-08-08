package com.ai.server.agent;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.agent.tools.DeviceControlTools;
import com.ai.server.agent.tools.LogOperationTools;
import com.ai.server.config.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 系统控制智能体
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemControlAgent extends AbstractBaseAgent {

    private final LogOperationTools logOperationTools;
    private final DeviceControlTools deviceControlTools;

    @Override
    public String systemMessage() {
        return SystemConstant.SYSTEM_CONTROL;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.SYSTEM_CONTROL;
    }

    @Override
    public Object[] tools() {
        return new Object[]{logOperationTools, deviceControlTools};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of("requestId", requestId, "sessionId", sessionId);
    }

    @Override
    protected void beforeProcessStream(String question, String sessionId, UUID userId, String requestId) {
        log.info("[SYSTEM CONTROL] 收到请求: sessionId={}, userId={}, question={}", sessionId, userId, question);
    }

    @Override
    protected void afterProcessStream(String sessionId, UUID userId, String finalContent) {
        log.info("[SYSTEM CONTROL] 响应完成: sessionId={}, responseLength={}",
                sessionId, finalContent == null ? 0 : finalContent.length());
    }
}
