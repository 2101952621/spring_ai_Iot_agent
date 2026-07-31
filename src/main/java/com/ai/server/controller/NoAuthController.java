package com.ai.server.controller;

import com.ai.server.security.auth.ResetPasswordRequest;
import com.ai.server.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/noauth")
@RequiredArgsConstructor
public class NoAuthController {

    private final UserService userService;

    @GetMapping("/activate")
    public ResponseEntity<?> checkActivateToken(@RequestParam("activateToken") String activateToken) {
        String email = userService.checkActivateToken(activateToken);
        return ResponseEntity.ok(Map.of("message", "有效的激活链接", "email", email));
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateUser(@RequestBody Map<String, String> request) {
        userService.activateUser(request.get("activateToken"));
        return ResponseEntity.ok(Map.of("message", "账号激活成功"));
    }

    @PostMapping("/resetPasswordByEmail")
    public ResponseEntity<?> resetPasswordByEmail(@RequestBody Map<String, String> request) {
        userService.sendResetPasswordMail(request.get("email"));
        return ResponseEntity.ok(Map.of("message", "密码重置邮件已发送"));
    }

    @GetMapping("/resetPassword")
    public ResponseEntity<?> checkResetToken(@RequestParam("resetToken") String resetToken) {
        String email = userService.checkResetToken(resetToken);
        return ResponseEntity.ok(Map.of("message", "有效的重置链接", "email", email));
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "密码重置成功"));
    }
}
