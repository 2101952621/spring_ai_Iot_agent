package com.ai.server.controller;

import com.ai.server.model.vo.ChatSessionVO;
import com.ai.server.model.vo.MessageVO;
import com.ai.server.model.vo.UserSessionVO;
import com.ai.server.security.SecurityUser;
import com.ai.server.security.SecurityUtils;
import com.ai.server.service.ai.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    public UserSessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        return this.chatSessionService.createSession(num, currentUser);
    }

    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        return this.chatSessionService.queryBySessionId(sessionId, currentUser);
    }

    @GetMapping("/history")
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        SecurityUser currentUser = SecurityUtils.currentUser();
        return this.chatSessionService.queryHistorySession(currentUser);
    }

    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        this.chatSessionService.deleteHistorySession(sessionId, currentUser);
    }

    @PutMapping("/history")
    public void updateTitle(@RequestParam("sessionId") String sessionId,
                            @RequestParam("title") String title) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        this.chatSessionService.updateTitle(sessionId, title, currentUser);
    }
}
