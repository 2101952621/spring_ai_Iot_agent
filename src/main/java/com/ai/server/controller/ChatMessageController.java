package com.ai.server.controller;

import com.ai.server.model.vo.ChatMessageSearchVO;
import com.ai.server.security.SecurityUser;
import com.ai.server.security.SecurityUtils;
import com.ai.server.service.ai.ChatMessageSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageSearchService chatMessageSearchService;

    /**
     * ES 全文检索历史消息
     */
    @GetMapping("/search/messages")
    public Map<String, Object> searchHistoryMessages(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        String userId = currentUser.getUuidId().toString();
        List<ChatMessageSearchVO> items = chatMessageSearchService.searchByKeyword(userId, keyword, page, size);
        long total = chatMessageSearchService.countByKeyword(userId, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("items", items);
        return result;
    }
}
