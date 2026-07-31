package com.ai.server.repository;

import com.ai.server.model.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 根据会话id查询所有消息，按消息索引升序排列
     */
    List<ChatMessageEntity> findByConversationIdOrderByMessageIndexAsc(String conversationId);

    /**
     * 删除指定会话的所有消息
     */
    @Modifying
    @Transactional
    void deleteByConversationId(String conversationId);

    /**
     * 查询所有不重复的会话id
     */
    @Query("SELECT DISTINCT cm.conversationId FROM ChatMessageEntity cm")
    List<String> findDistinctConversationIds();

    @Modifying
    @Transactional
    void deleteByConversationIdAndMessageContent(String conversationId, String agentName);

    List<ChatMessageEntity> findAllByConversationId(String conversationId);
}
