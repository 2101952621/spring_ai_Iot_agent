package com.ai.server.agent;

import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.config.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 知识答疑智能体 — 纯知识库问答
 * <pre>
 * 职责：
 *   - 解答网络设备、物联网相关知识问题
 *   - 基于 System Prompt 约束回答范围
 *
 * 区别于 RecommendBaseAgent：
 *   - KnowledgeBaseAgent：纯知识答疑，无需 RAG 检索和 Tool 调用
 *   - RecommendBaseAgent：设备推荐，需要 RAG 检索 + Tool 调用
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseAgent extends AbstractBaseAgent {

    @Override
    public String systemMessage() {
        return SystemConstant.KNOWLEDGE;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

    /**
     * 流式处理前 — 记录知识问答请求
     */
    @Override
    protected void beforeProcessStream(String question, String sessionId, UUID userId, String requestId) {
        log.info("知识问答智能体收到请求: sessionId={}, requestId={}, question={}",
                sessionId, requestId, question);
    }

    /**
     * 流式处理后 — 记录问答完成
     */
    @Override
    protected void afterProcessStream(String sessionId, UUID userId, String finalContent) {
        log.info("知识问答智能体完成响应: sessionId={}, responseLength={}",
                sessionId, finalContent.length());
    }
}
