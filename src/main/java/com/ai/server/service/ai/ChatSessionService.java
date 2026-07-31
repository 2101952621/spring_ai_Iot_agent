package com.ai.server.service.ai;

import com.ai.server.model.vo.ChatSessionVO;
import com.ai.server.model.vo.MessageVO;
import com.ai.server.model.vo.UserSessionVO;
import com.ai.server.model.entity.ChatSessionEntity;
import com.ai.server.security.SecurityUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ChatSessionService {

    List<ChatSessionEntity> getChatSessionsByUserId(UUID userId);

    ChatSessionEntity getChatSessionBySessionId(String sessionId);

    void update(String sessionId, String question, UUID userId);

    UserSessionVO createSession(Integer num, SecurityUser currentUser);

    List<MessageVO> queryBySessionId(String sessionId, SecurityUser currentUser);

    Map<String, List<ChatSessionVO>> queryHistorySession(SecurityUser currentUser);

    void deleteHistorySession(String sessionId, SecurityUser currentUser);

    void updateTitle(String sessionId, String title, SecurityUser currentUser);
}
