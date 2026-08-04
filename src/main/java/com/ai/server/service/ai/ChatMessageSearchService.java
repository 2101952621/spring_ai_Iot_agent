package com.ai.server.service.ai;

import com.ai.server.model.entity.ChatMessageEntity;
import com.ai.server.model.vo.ChatMessageSearchVO;

import java.util.List;

/**
 * 聊天消息 ES 搜索服务
 */
public interface ChatMessageSearchService {

    /**
     * 初始化 ES 索引（创建映射，已存在则跳过）
     */
    void ensureIndex();

    /**
     * 单条消息写入 ES
     *
     * @param entity 消息实体（必须包含 PG id）
     */
    void indexMessage(ChatMessageEntity entity);

    /**
     * 批量写入 ES（整段会话同步）
     *
     * @param entities 消息实体列表
     */
    void indexBatch(List<ChatMessageEntity> entities);

    /**
     * 删除会话在 ES 中的所有消息
     *
     * @param conversationId 会话ID（userId_sessionId）
     */
    void deleteByConversationId(String conversationId);

    /**
     * 按关键字搜索当前用户的历史消息
     *
     * @param userId  当前用户ID（用于隔离）
     * @param keyword 关键字（为空时返回空列表）
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @return 命中消息列表（按相关度+时间倒序）
     */
    List<ChatMessageSearchVO> searchByKeyword(String userId, String keyword, int page, int size);

    /**
     * 统计命中数量
     */
    long countByKeyword(String userId, String keyword);
}