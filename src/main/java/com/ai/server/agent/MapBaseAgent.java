package com.ai.server.agent;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.config.McpConfig;
import com.ai.server.config.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 地图出行智能体 — 高德地图 MCP 服务接入
 */
@Slf4j
@Component
public class MapBaseAgent extends AbstractBaseAgent {

    private final ToolCallback[] amapMcpTools;

    public MapBaseAgent(@Qualifier(McpConfig.AMAP_MCP_TOOLS) ToolCallback[] amapMcpTools) {
        this.amapMcpTools = amapMcpTools;
    }

    @Override
    public String systemMessage() {
        return SystemConstant.MAP;
    }

    @Override
    public ToolCallback[] toolCallbacks() {
        return amapMcpTools;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.MAP;
    }

    /**
     * 流式处理前 — 记录地图服务请求
     */
    @Override
    protected void beforeProcessStream(String question, String sessionId, UUID userId, String requestId) {
        log.info("地图出行智能体收到请求: sessionId={}, requestId={}, question={}, mcpToolCount={}",
                sessionId, requestId, question, amapMcpTools.length);
    }

    /**
     * 流式处理后 — 记录响应完成
     */
    @Override
    protected void afterProcessStream(String sessionId, UUID userId, String finalContent) {
        log.info("地图出行智能体完成响应: sessionId={}, responseLength={}",
                sessionId, finalContent.length());
    }
}
