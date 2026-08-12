package com.ai.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * MCP 客户端配置 — 高德地图 MCP 服务接入
 */
@Slf4j
@Configuration
public class McpConfig {

    /**
     * 高德地图 MCP 工具数组的 Bean 名称
     */
    public static final String AMAP_MCP_TOOLS = "amapMcpTools";

    /**
     * 聚合所有 MCP 客户端（当前仅高德 amap）的工具回调
     *
     * @param providers Spring AI MCP 自动配置注册的 ToolCallbackProvider
     * @return MCP 工具回调数组
     */
    @Bean(AMAP_MCP_TOOLS)
    public ToolCallback[] amapMcpTools(ObjectProvider<ToolCallbackProvider> providers) {
        return providers.orderedStream()
                .map(ToolCallbackProvider::getToolCallbacks)
                .flatMap(Arrays::stream)
                .toArray(ToolCallback[]::new);
    }
}
