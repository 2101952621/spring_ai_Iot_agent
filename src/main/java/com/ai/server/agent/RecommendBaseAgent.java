package com.ai.server.agent;

import com.ai.server.agent.tools.DeviceBaseInfoTools;
import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.config.SystemConstant;
import com.ai.server.model.entity.DeviceBaseInfoEntity;
import com.ai.server.repository.DeviceBaseInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final DeviceBaseInfoRepository deviceBaseInfoRepository;

    /**
     * 启动时从 device_base_info 表加载设备数据，初始化向量存储
     * <p>
     * 将设备的核心推荐字段（名称、型号、分类、适用场景、核心功能、详情等）
     * 写入向量存储，供 RAG 检索时做语义匹配。
     */
    @PostConstruct
    public void initVectorStoreFromDB() {
        log.info("[VECTOR INIT] ===== 开始从 device_base_info 表加载设备数据到向量存储 =====");
        List<DeviceBaseInfoEntity> deviceList;
        try {
            deviceList = deviceBaseInfoRepository.findAll();
            log.info("[VECTOR INIT] 从数据库加载到 {} 条设备记录", deviceList.size());
        } catch (Exception e) {
            log.error("[VECTOR INIT] 从数据库加载设备数据失败: {}", e.getMessage(), e);
            return;
        }

        if (deviceList.isEmpty()) {
            log.warn("[VECTOR INIT] device_base_info 表为空，跳过向量存储初始化");
            return;
        }
        List<Document> documents = new ArrayList<>();
        for (DeviceBaseInfoEntity device : deviceList) {
            Document doc = buildDeviceDocument(device);
            documents.add(doc);
        }
        int batchSize = 20;
        int total = documents.size();
        int successCount = 0;
        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Document> batch = documents.subList(i, end);
            try {
                this.vectorStore.add(batch);
                successCount += batch.size();
                log.info("[VECTOR INIT] 批次写入成功 [{}-{}/{}]", i + 1, end, total);
            } catch (Exception e) {
                log.error("[VECTOR INIT] 批次写入失败 [{}-{}]: {}", i + 1, end, e.getMessage());
            }
        }
        log.info("[VECTOR INIT] 向量存储写入完成: 成功 {}/{} 条", successCount, total);

        if (successCount == 0) {
            log.error("[VECTOR INIT] 所有批次写入均失败，终止初始化");
            return;
        }
        log.info("[VECTOR INIT] ===== 向量存储健康检查 =====");
        String[] testQueries = {"SOHO办公", "路由器", "Wi-Fi", "高端家庭", "交换机", "办公室"};
        for (String query : testQueries) {
            try {
                List<Document> results = this.vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).similarityThreshold(0.4d).topK(3).build());
                log.info("[VECTOR INIT] 关键词='{}' 命中 {} 条", query, results.size());
                for (Document doc : results) {
                    String preview = doc.getText() != null
                            ? doc.getText().replace("\n", " | ").substring(0, Math.min(120, doc.getText().length()))
                            : "null";
                    log.info("[VECTOR INIT]   score={} id={} content={}", doc.getScore(), doc.getId(), preview);
                }
            } catch (Exception e) {
                log.warn("[VECTOR INIT] 向量存储探测异常: query={}, error={}", query, e.getMessage());
            }
        }
        log.info("[VECTOR INIT] ===== 向量存储初始化完毕 =====");
    }

    /**
     * 将单条设备实体转换为向量存储文档
     * <p>
     * 按照 {@link SystemConstant#RECOMMEND} 推荐词中定义的上下文结构组织文本：
     * 设备ID、名称、型号、分类、适用场景、核心功能
     *
     * @param device 设备实体
     * @return 向量存储文档
     */
    private Document buildDeviceDocument(DeviceBaseInfoEntity device) {
        String text = "设备名称: " + nullToEmpty(device.getDeviceName()) + "\n" +
                "设备型号: " + nullToEmpty(device.getDeviceModel()) + "\n" +
                "设备类型: " + nullToEmpty(device.getDeviceType()) + "\n" +
                "产品分类: " + nullToEmpty(device.getCategory()) + "\n" +
                "适用场景: " + nullToEmpty(device.getSuitableScenarios()) + "\n" +
                "核心功能: " + nullToEmpty(device.getCore()) + "\n" +
                "产品详情: " + nullToEmpty(device.getDetail()) + "\n";
        Map<String, Object> metadata = getStringObjectMap(device);
        return new Document(String.valueOf(device.getId()), text, metadata);
    }

    private Map<String, Object> getStringObjectMap(DeviceBaseInfoEntity device) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("device_id", device.getId());
        metadata.put("device_type", nullToEmpty(device.getDeviceType()));
        metadata.put("category", nullToEmpty(device.getCategory()));
        metadata.put("device_name", nullToEmpty(device.getDeviceName()));
        metadata.put("device_model", nullToEmpty(device.getDeviceModel()));
        if (device.getPrice() != null) {
            metadata.put("price", device.getPrice().doubleValue());
        }
        return metadata;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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
                        .similarityThreshold(0.4d)
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
