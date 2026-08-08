package com.ai.server.agent.tools;

import cn.hutool.core.util.StrUtil;
import com.ai.server.agent.tools.result.WebFunctionInfo;
import com.ai.server.config.ToolResultUtils;
import com.ai.server.model.entity.WebFunctionEntity;
import com.ai.server.repository.WebFunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网页功能查询工具
 * <p>
 * 根据用户自然语言（功能名称/关键词）查询匹配的可打开网页功能，
 * 并把结果存入 ToolResultUtils，前端通过 CARD 事件渲染"打开"卡片。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebFunctionTools {

    public static final String TOOL_RESULT_KEY = "webFunction";
    public static final String BASE_URL_FALLBACK = "https://test.com.cn";

    private final WebFunctionRepository webFunctionRepository;

    @Tool(description = "查询可打开的Web功能配置：根据用户提到的功能名称或关键词（如'设备登录密码'、'历史版本'、" +
            "'Web登录'等），从功能配置表中匹配对应功能，返回功能名称、完整打开URL、按钮文案、描述等。" +
            "返回的字段中将通过特殊卡片消息推送给前端展示'打开'按钮。")
    public List<WebFunctionInfo> queryWebFunctionByName(@ToolParam(description = "用户提到的功能名称或关键词") String keyword,
            ToolContext toolContext) {

        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }
        List<WebFunctionInfo> matches = webFunctionRepository.searchEnabledByKeyword(keyword.trim())
                .stream()
                .map(this::convert)
                .toList();
        if (!matches.isEmpty()) {
            String requestId = resolveRequestId(toolContext);
            if (requestId != null) {
                ToolResultUtils.put(requestId, TOOL_RESULT_KEY, matches.get(0));
                log.info("[WEB OPEN] 命中 {} 个功能，已写入 ToolResult: requestId={}, topMatch={}",
                        matches.size(), requestId, matches.get(0).getFunctionName());
            } else {
                log.warn("[WEB OPEN] ToolContext 中未找到 requestId，无法写入 CARD 事件");
            }
        }
        return matches;
    }

    private WebFunctionInfo convert(WebFunctionEntity e) {
        return WebFunctionInfo.builder()
                .id(e.getId())
                .functionCode(e.getFunctionCode())
                .functionName(e.getFunctionName())
                .module(e.getModule())
                .openUrl(resolveOpenUrl(e))
                .buttonText(e.getButtonText() != null ? e.getButtonText() : "打开")
                .icon(e.getIcon())
                .description(e.getDescription())
                .cardType(e.getCardType() != null ? e.getCardType() : "WEB_FUNCTION")
                .precautions(e.getPrecautions())
                .configMethod(e.getConfigMethod())
                .build();
    }

    private String resolveOpenUrl(WebFunctionEntity e) {
        if (StrUtil.isNotBlank(e.getOpenUrl())) {
            return e.getOpenUrl();
        }
        String base = StrUtil.isNotBlank(e.getBaseUrl()) ? e.getBaseUrl() : BASE_URL_FALLBACK;
        String path = e.getFunctionPath();
        if (StrUtil.isBlank(path)) return base;
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        if (base.endsWith("/") && path.startsWith("/")) {
            return base + path.substring(1);
        }
        return base + path;
    }

    @SuppressWarnings("unchecked")
    private String resolveRequestId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) return null;
        Object v = toolContext.getContext().get("requestId");
        return v != null ? v.toString() : null;
    }
}