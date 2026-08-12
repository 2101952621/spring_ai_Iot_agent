package com.ai.server.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.server.agent.enums.UserChatEventType;
import com.ai.server.agent.tools.LogOperationTools;
import com.ai.server.agent.tools.WebFunctionTools;
import com.ai.server.config.Constant;
import com.ai.server.config.ToolResultUtils;
import com.ai.server.model.vo.ChatEventVO;
import com.ai.server.service.RedisService;
import com.ai.server.service.ai.AgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 智能体抽象基类 — 基于模板方法模式
 * <pre>
 * 模板方法钩子（子类可按需覆盖）：
 *   beforeProcessStream()  — 流式处理前的准备
 *   afterProcessStream()   — 流式处理后的清理
 *   onStreamCancel()       — 用户主动取消时的回调
 *   onStreamComplete()     — 流式正常完成时的回调
 *   generateRequestId()    — 自定义请求ID生成策略
 *   buildSystemMessage()   — 构建系统消息（委托到 systemMessage()）
 * </pre>
 */
@Slf4j
public abstract class AbstractBaseAgent implements BaseAgent {

    @Lazy
    @Resource
    protected AgentService agentService;
    @Resource
    protected ChatClient chatClient;
    @Resource
    protected ChatMemory chatMemory;
    @Resource
    protected RedisService redisService;

    private static final String GENERATE_STATUS_KEY_PREFIX = "chat:generate_status:";
    private static final long GENERATE_STATUS_TIMEOUT_MINUTES = 5;

    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(UserChatEventType.STOP.getValue())
            .build();

    /**
     * 同步处理（用于意图识别等非流式场景）
     * 注意：不再调用 agentService.update()，由 Orchestrator 统一管理
     */
    @Override
    public String process(String question, String sessionId, UUID userId) {
        var requestId = generateRequestId();
        var conversationId = agentService.getConversationId(userId, sessionId);
        return getChatClientRequest(userId, sessionId, requestId, conversationId, question)
                .call()
                .content();
    }

    /**
     * 流式处理 — 模板方法，子类可通过钩子干预流程
     */
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId, UUID userId) {
        var requestId = generateRequestId();
        var conversationId = agentService.getConversationId(userId, sessionId);
        var outputBuilder = new StringBuilder();
        beforeProcessStream(question, sessionId, userId, requestId);
        return getChatClientRequest(userId, sessionId, requestId, conversationId, question)
                .stream()
                .chatResponse()
                .doFirst(() ->
                        redisService.setCacheObject(generateStatusKey(sessionId), true,
                                GENERATE_STATUS_TIMEOUT_MINUTES, TimeUnit.MINUTES))
                .doOnCancel(() -> {
                    saveStopHistoryRecord(conversationId, outputBuilder.toString());
                    onStreamCancel(sessionId, userId, outputBuilder.toString());
                })
                .takeWhile(response ->
                        Boolean.TRUE.equals(redisService.getCacheObject(generateStatusKey(sessionId))))
                .concatMap(chatResponse ->
                        transformChatResponse(chatResponse, outputBuilder, requestId))
                .doFinally(signalType -> {
                    redisService.deleteObject(generateStatusKey(sessionId));
                    afterProcessStream(sessionId, userId, outputBuilder.toString());
                });
    }

    /**
     * 流式处理前的钩子，子类可在此做预处理（如参数校验、埋点等）
     */
    protected void beforeProcessStream(String question, String sessionId, UUID userId, String requestId) {
        // 默认空实现
    }

    /**
     * 流式处理后的钩子，子类可在此做后处理（如数据统计、日志记录等）
     */
    protected void afterProcessStream(String sessionId, UUID userId, String finalContent) {
        // 默认空实现
    }

    /**
     * 用户主动取消流式响应时的钩子
     */
    protected void onStreamCancel(String sessionId, UUID userId, String partialContent) {
        log.info("流式响应被取消: sessionId={}, partialContentLength={}", sessionId, partialContent.length());
    }

    /**
     * 流式正常完成时的钩子（在 doFinally 之前触发）
     */
    protected void onStreamComplete(String sessionId, UUID userId, String fullContent) {
        log.info("流式响应完成: sessionId={}, fullContentLength={}", sessionId, fullContent.length());
    }

    /**
     * 输出文本转换钩子：子类可覆盖此方法对 AI 输出的每段文本做后处理
     * （如修正截断、统一格式化等），默认原样返回。
     */
    protected String transformOutputText(String text, String requestId) {
        return text;
    }

    /**
     * 生成请求ID，子类可覆盖以自定义ID生成策略
     */
    protected String generateRequestId() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 构建 ChatClient 请求（protected 暴露给子类复用）
     */
    protected ChatClient.ChatClientRequestSpec getChatClientRequest(
            UUID userId, String sessionId, String requestId,
            String conversationId, String question) {
        return chatClient.prompt()
                .system(promptSystem ->
                        promptSystem.text(systemMessage()).params(systemMessageParams()))
                .advisors(advisor ->
                        advisor.advisors(advisors())
                                .params(advisorParams(userId, sessionId, requestId)))
                .tools(tools())
                .toolCallbacks(toolCallbacks())
                .toolContext(toolContext(sessionId, requestId))
                .user(question);
    }

    /**
     * 转换 ChatResponse → ChatEventVO 流
     */
    private Flux<ChatEventVO> transformChatResponse(ChatResponse chatResponse,
                                                    StringBuilder outputBuilder, String requestId) {

        Generation result = chatResponse.getResult();
        if (result == null) {
            return Flux.empty();
        }

        var metadata = result.getMetadata();
        var finishReason = metadata != null ? metadata.getFinishReason() : null;
        var isStop = StrUtil.equals(Constant.STOP, finishReason);
        if (isStop) {
            var responseMetadata = chatResponse.getMetadata();
            if (responseMetadata != null) {
                ToolResultUtils.put(responseMetadata.getId(), Constant.REQUEST_ID, requestId);
            }
        }
        var output = result.getOutput();
        var text = output != null ? output.getText() : null;
        if (text != null) {
            text = transformOutputText(text, requestId);
            outputBuilder.append(text);
        }
        if (StrUtil.isEmpty(text)) {
            return isStop ? Flux.just(STOP_EVENT) : Flux.empty();
        }
        var dataEvent = ChatEventVO.builder()
                .eventData(text)
                .eventType(UserChatEventType.DATA.getValue())
                .build();
        if (isStop) {
            return Flux.just(dataEvent, STOP_EVENT);
        }
        var toolResultMap = ToolResultUtils.get(requestId);
        if (CollUtil.isEmpty(toolResultMap)) {
            return Flux.just(dataEvent);
        }
        ToolResultUtils.remove(requestId);
        return Flux.fromIterable(buildToolResultEvents(dataEvent, toolResultMap));
    }

    protected List<ChatEventVO> buildToolResultEvents(ChatEventVO dataEvent,
                                                      Map<String, Object> toolResultMap) {
        List<ChatEventVO> events = new ArrayList<>(6);
        events.add(dataEvent);

        Object webFunction = toolResultMap.get(WebFunctionTools.TOOL_RESULT_KEY);
        if (webFunction != null) {
            events.add(ChatEventVO.builder()
                    .eventType(UserChatEventType.CARD.getValue())
                    .eventData(webFunction)
                    .build());
        }

        Object logExport = toolResultMap.get(LogOperationTools.TOOL_RESULT_KEY_EXPORT);
        if (logExport != null) {
            events.add(ChatEventVO.builder()
                    .eventType(UserChatEventType.SYSTEM_DOWNLOAD.getValue())
                    .eventData(logExport)
                    .build());
        }
        Object logReport = toolResultMap.get(LogOperationTools.TOOL_RESULT_KEY_REPORT);
        if (logReport != null) {
            events.add(ChatEventVO.builder()
                    .eventType(UserChatEventType.SYSTEM_DOWNLOAD.getValue())
                    .eventData(logReport)
                    .build());
        }
        Object analysisPreview = toolResultMap.get(LogOperationTools.TOOL_RESULT_KEY_ANALYSIS);
        if (analysisPreview != null) {
            events.add(ChatEventVO.builder()
                    .eventType(UserChatEventType.SYSTEM_ANALYSIS.getValue())
                    .eventData(analysisPreview)
                    .build());
        }
        events.add(STOP_EVENT);
        return events;
    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }

    @Override
    public Map<String, Object> advisorParams(UUID userId, String sessionId, String requestId) {
        var conversationId = agentService.getConversationId(userId, sessionId);
        return Map.of("chat_memory_conversation_id", conversationId);
    }

    @Override
    public void stop(String sessionId) {
        redisService.deleteObject(generateStatusKey(sessionId));
    }

    private static String generateStatusKey(String sessionId) {
        return GENERATE_STATUS_KEY_PREFIX + sessionId;
    }
}
