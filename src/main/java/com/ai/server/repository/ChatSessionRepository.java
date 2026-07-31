package com.ai.server.repository;

import com.ai.server.model.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {

    ChatSessionEntity findBySessionId(String sessionId);

    List<ChatSessionEntity> findByUserIdOrderByUpdateTimeDesc(UUID userId);
}
