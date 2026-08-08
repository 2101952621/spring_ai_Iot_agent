package com.ai.server.agent;

import cn.hutool.core.util.StrUtil;
import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.agent.tools.WebFunctionTools;
import com.ai.server.config.SystemConstant;
import com.ai.server.config.ToolResultUtils;
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

    /** 命中功能时前端期望的固定引导语（与 SystemConstant.WEB_OPEN 中的输出格式保持一致） */
    static final String HIT_MESSAGE = "找到相关功能，点击下方按钮即可进入该功能页面";
    /** 未命中功能时前端期望的固定引导语 */
    static final String MISS_MESSAGE = "未找到相关功能，请尝试其他关键词";

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

    /**
     * 兜底修正：AI 模型可能截断或改写固定输出文本，此处强制替换为期望值。
     */
    @Override
    protected String transformOutputText(String text, String requestId) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        if (HIT_MESSAGE.equals(text) || MISS_MESSAGE.equals(text)) {
            return text;
        }
        Map<String, Object> results = ToolResultUtils.get(requestId);
        if (results != null && results.containsKey(WebFunctionTools.TOOL_RESULT_KEY)) {
            log.warn("[WEB OPEN] AI输出被截断/改写，已修正为命中文本。requestId={}, AI输出={}", requestId, text);
            return HIT_MESSAGE;
        }
        log.debug("[WEB OPEN] 文本无需修正: requestId={}, text={}", requestId, text);
        return text;
    }
}