package com.ai.server.service.ai;

import com.ai.server.model.entity.ChatMessageEntity;
import com.ai.server.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageSearchService chatMessageSearchService;

    public List<String> findConversationIds() {
        return chatMessageRepository.findDistinctConversationIds();
    }

    public List<ChatMessageEntity> findByConversationId(String conversationId) {
        return chatMessageRepository.findByConversationIdOrderByMessageIndexAsc(conversationId);
    }

    @Transactional
    public void saveAll(String conversationId, List<ChatMessageEntity> entities) {
        chatMessageRepository.saveAll(entities);
        chatMessageSearchService.indexBatch(entities);
    }

    @Transactional
    public void deleteByConversationId(String conversationId) {
        chatMessageRepository.deleteByConversationId(conversationId);
        chatMessageSearchService.deleteByConversationId(conversationId);
    }

    @Transactional
    public void optimization(String conversationId, String agentName) {
        List<ChatMessageEntity> messages = chatMessageRepository.findAllByConversationId(conversationId);
        if (messages.isEmpty()) {
            return;
        }
        messages.sort(Comparator.comparingInt(ChatMessageEntity::getMessageIndex).reversed());
        int deleteCount = 0;
        for (ChatMessageEntity message : messages) {
            if (deleteCount >= 2) break;
            chatMessageRepository.delete(message);
            deleteCount++;
        }
        log.info("优化对话记录, conversationId={}, 删除最后{}条路由消息", conversationId, deleteCount);
        List<ChatMessageEntity> remaining = chatMessageRepository.findAllByConversationId(conversationId);
        chatMessageSearchService.deleteByConversationId(conversationId);
        if (!remaining.isEmpty()) {
            chatMessageSearchService.indexBatch(remaining);
        }
    }
}
