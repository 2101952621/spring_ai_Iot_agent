package com.ai.server.agent;

import com.ai.server.agent.tools.DeviceBaseInfoTools;
import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.config.Constant;
import com.ai.server.config.SystemConstant;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 设备推荐智能体 — RAG 向量检索 + Tool Calling
 * <pre>
 * 能力组合：
 *   1. RAG 检索（QuestionAnswerAdvisor 连接 VectorStore）
 *   2. Function Calling（DeviceBaseInfoTools 查询设备详情）
 *
 * 执行流程：
 *   beforeProcessStream → RAG检索 → LLM推理 → Tool调用 → 流式输出 → afterProcessStream
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendBaseAgent extends AbstractBaseAgent {

    private final VectorStore vectorStore;
    private final DeviceBaseInfoTools deviceBaseInfoTools;

    /**
     * 启动时进行向量存储健康检查
     */
    @PostConstruct
    public void debugVectorStore() {
        log.info("[RAG DEBUG] ===== 向量存储启动检查 =====");
        String[] testQueries = {"SOHO办公", "路由器", "Wi-Fi", "高端家庭"};
        for (String query : testQueries) {
            try {
                List<Document> results = this.vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).similarityThreshold(0.4d).topK(3).build());
                log.info("[RAG DEBUG] 关键词='{}' 命中 {} 条", query, results.size());
                for (Document doc : results) {
                    String preview = doc.getText() != null
                            ? doc.getText().replace("\n", " | ").substring(0, Math.min(120, doc.getText().length()))
                            : "null";
                    log.info("[RAG DEBUG]   score={} id={} content={}", doc.getScore(), doc.getId(), preview);
                }
            } catch (Exception e) {
                log.warn("[RAG DEBUG] 向量存储探测异常: query={}, error={}", query, e.getMessage());
            }
        }
        log.info("[RAG DEBUG] ===== 向量存储检查完毕 =====");
    }

    @Override
    public String systemMessage() {
        return SystemConstant.RECOMMEND;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.RECOMMEND;
    }

    @Override
    public List<Advisor> advisors() {
        var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d)
                        .topK(6)
                        .build())
                .build();
        return List.of(qaAdvisor);
    }

    @Override
    public Object[] tools() {
        return new Object[]{deviceBaseInfoTools};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of(
                "userId", "",
                "requestId", requestId
        );
    }

    /**
     * 流式处理前 — 记录推荐请求
     */
    @Override
    protected void beforeProcessStream(String question, String sessionId, UUID userId, String requestId) {
        log.info("推荐智能体收到请求: sessionId={}, requestId={}, question={}",
                sessionId, requestId, question);
    }

    /**
     * 流式处理后 — 记录推荐完成
     */
    @Override
    protected void afterProcessStream(String sessionId, UUID userId, String finalContent) {
        log.info("推荐智能体完成响应: sessionId={}, responseLength={}",
                sessionId, finalContent.length());
    }
}
