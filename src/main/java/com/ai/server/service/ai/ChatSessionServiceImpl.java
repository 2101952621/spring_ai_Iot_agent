package com.ai.server.service.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import com.ai.server.agent.enums.AgentTypeEnum;
import com.ai.server.agent.enums.SystemMessageType;
import com.ai.server.model.vo.ChatSessionVO;
import com.ai.server.model.vo.HotExampleVO;
import com.ai.server.model.vo.MessageVO;
import com.ai.server.model.vo.UserSessionVO;
import com.ai.server.model.entity.ChatMessageEntity;
import com.ai.server.model.entity.ChatSessionEntity;
import com.ai.server.repository.ChatMessageRepository;
import com.ai.server.repository.ChatSessionRepository;
import com.ai.server.repository.HotExampleRepository;
import com.ai.server.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对话session Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HotExampleRepository hotExampleRepository;

    @Override
    public List<ChatSessionEntity> getChatSessionsByUserId(UUID userId) {
        log.debug("查询用户历史会话列表, userId={}", userId);
        return chatSessionRepository.findByUserIdOrderByUpdateTimeDesc(userId);
    }

    @Override
    public ChatSessionEntity getChatSessionBySessionId(String sessionId) {
        log.debug("查询会话详细信息, sessionId={}", sessionId);
        return chatSessionRepository.findBySessionId(sessionId);
    }

    @Override
    public void update(String sessionId, String question, UUID userId) {
        ChatSessionEntity bySessionId = chatSessionRepository.findBySessionId(sessionId);
        if (bySessionId == null) {
            bySessionId = new ChatSessionEntity();
            bySessionId.setSessionId(sessionId);
            bySessionId.setId(UUID.randomUUID());
            bySessionId.setUserId(userId);
            bySessionId.setTitle(question.substring(0, Math.min(question.length(), 20)));
            bySessionId.setCreateTime(new Date());
            bySessionId.setUpdateTime(new Date());
        } else {
            if (bySessionId.getTitle() == null || bySessionId.getTitle().isBlank()) {
                bySessionId.setTitle(question.substring(0, Math.min(question.length(), 20)));
            }
            bySessionId.setUpdateTime(new Date());
        }
        chatSessionRepository.save(bySessionId);
    }

    @Override
    public UserSessionVO createSession(Integer num, SecurityUser currentUser) {
        UserSessionVO userSessionVO = new UserSessionVO();
        userSessionVO.setHotExamples(hotExampleRepository.findAll(Pageable.ofSize(3))
                .stream().map(HotExampleVO::new).toList());
        userSessionVO.setSessionId(IdUtil.fastSimpleUUID());

        var chatSession = ChatSessionEntity.builder()
                .id(UUID.randomUUID())
                .createTime(new Date())
                .updateTime(new Date())
                .sessionId(userSessionVO.getSessionId())
                .userId(currentUser.getUuidId())
                .build();
        chatSessionRepository.save(chatSession);
        return userSessionVO;
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId, SecurityUser currentUser) {
        String conversationId = getConversationId(sessionId, currentUser.getUuidId());
        List<ChatMessageEntity> messageList = chatMessageRepository.findAllByConversationId(conversationId);
        return StreamUtil.of(messageList)
                .filter(message -> Objects.equals(message.getMessageType(), MessageType.ASSISTANT.name()) || Objects.equals(message.getMessageType(), MessageType.USER.name()))
                .map(message -> MessageVO.builder()
                        .content(message.getMessageContent())
                        .type(SystemMessageType.valueOf(message.getMessageType()))
                        .build())
                .toList();
    }

    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession(SecurityUser currentUser) {
        UUID userId = currentUser.getUuidId();
        List<ChatSessionEntity> allSessions = chatSessionRepository.findByUserIdOrderByUpdateTimeDesc(userId);
        if (CollUtil.isEmpty(allSessions)) {
            return Map.of("最近一周", Collections.emptyList(), "一个月内", Collections.emptyList());
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusDays(7);
        LocalDateTime oneMonthAgo = now.minusDays(30);
        Map<Boolean, List<ChatSessionEntity>> partitioned = allSessions.stream()
                .collect(Collectors.partitioningBy(session -> {
                    Date updateTime = session.getUpdateTime();
                    if (updateTime == null) return false;
                    LocalDateTime ut = updateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    return !ut.isBefore(oneWeekAgo);
                }));
        List<ChatSessionVO> weekList = partitioned.getOrDefault(true, Collections.emptyList())
                .stream().map(this::toChatSessionVO).toList();
        List<ChatSessionVO> monthList = partitioned.getOrDefault(false, Collections.emptyList())
                .stream()
                .filter(session -> {
                    Date updateTime = session.getUpdateTime();
                    if (updateTime == null) return false;
                    LocalDateTime ut = updateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    return !ut.isBefore(oneMonthAgo);
                })
                .map(this::toChatSessionVO).toList();
        Map<String, List<ChatSessionVO>> result = new LinkedHashMap<>();
        result.put("最近一周", weekList);
        result.put("一个月内", monthList);
        return result;
    }

    @Override
    public void deleteHistorySession(String sessionId, SecurityUser currentUser) {
        ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null) {
            log.warn("删除历史会话失败，会话不存在: sessionId={}", sessionId);
            return;
        }
        if (!Objects.equals(session.getUserId(), currentUser.getUuidId())) {
            log.warn("删除历史会话失败，无权操作: sessionId={}, userId={}", sessionId, currentUser.getUuidId());
            return;
        }
        String conversationId = getConversationId(sessionId, currentUser.getUuidId());
        chatMessageRepository.deleteByConversationId(conversationId);
        chatSessionRepository.delete(session);
        log.info("删除历史会话成功: sessionId={}, userId={}", sessionId, currentUser.getUuidId());
    }

    @Override
    public void updateTitle(String sessionId, String title, SecurityUser currentUser) {
        ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null) {
            log.warn("更新会话标题失败，会话不存在: sessionId={}", sessionId);
            return;
        }
        if (!Objects.equals(session.getUserId(), currentUser.getUuidId())) {
            log.warn("更新会话标题失败，无权操作: sessionId={}, userId={}", sessionId, currentUser.getUuidId());
            return;
        }
        session.setTitle(title);
        chatSessionRepository.save(session);
        log.info("更新会话标题成功: sessionId={}, title={}", sessionId, title);
    }

    private ChatSessionVO toChatSessionVO(ChatSessionEntity entity) {
        LocalDateTime updateTime = entity.getUpdateTime() != null
                ? entity.getUpdateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;
        return ChatSessionVO.builder()
                .sessionId(entity.getSessionId())
                .title(entity.getTitle())
                .updateTime(updateTime)
                .build();
    }

    static String getConversationId(String sessionId, UUID userId) {
        return userId + "_" + sessionId;
    }
}
