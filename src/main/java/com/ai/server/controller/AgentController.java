package com.ai.server.controller;

import com.ai.server.model.dto.ChatDTO;
import com.ai.server.model.vo.ChatEventVO;
import com.ai.server.security.SecurityUser;
import com.ai.server.security.SecurityUtils;
import com.ai.server.service.ai.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        return agentService.chat(chatDTO.getQuestion(), chatDTO.getSessionId(), currentUser);
    }

    @PostMapping("/stop/{sessionId}")
    public void chatStop(@PathVariable String sessionId) {
        agentService.chatStop(sessionId);
    }
}
