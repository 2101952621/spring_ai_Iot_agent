package com.ai.server.controller;

import com.ai.server.security.auth.RegisterRequest;
import com.ai.server.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;

    @PostMapping("/sendRegisterMail")
    public ResponseEntity<?> sendRegisterMail(@RequestBody Map<String, String> request) {
        userService.sendRegisterMail(request.get("email"));
        return ResponseEntity.ok(Map.of("message", "激活邮件已发送，请查收"));
    }

    @PostMapping("/registerByEmail")
    public ResponseEntity<?> registerByEmail(@Valid @RequestBody RegisterRequest request) {
        var userVO = userService.registerByEmail(request);
        return ResponseEntity.ok(Map.of("message", "注册成功，请前往邮箱激活账号", "user", userVO));
    }
}
