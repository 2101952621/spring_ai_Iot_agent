package com.ai.server.agent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 智能体类型枚举
 */
@Getter
@AllArgsConstructor
public enum AgentTypeEnum {

    /**
     * 路由智能体 — 负责意图识别与分发
     */
    ROUTE("ROUTE"),

    /**
     * 推荐智能体 — 设备推荐（RAG + Tool Calling）
     */
    RECOMMEND("RECOMMEND"),

    /**
     * 知识库智能体 — 纯知识问答
     */
    KNOWLEDGE("KNOWLEDGE"),

    /**
     * 兜底智能体 — 处理所有未匹配的请求
     */
    FALLBACK("FALLBACK");

    private final String agentName;

    /**
     * 根据名称字符串匹配枚举值
     */
    public static AgentTypeEnum agentNameOf(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (AgentTypeEnum value : values()) {
            if (Objects.equals(value.agentName, name.trim())) {
                return value;
            }
        }
        return null;
    }
}
