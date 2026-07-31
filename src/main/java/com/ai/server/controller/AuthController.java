package com.ai.server.controller;

import com.ai.server.security.auth.AuthResponse;
import com.ai.server.security.auth.LoginRequest;
import com.ai.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/public")
    public ResponseEntity<?> loginPublic(@Valid @RequestBody LoginRequest request) {
        return login(request);
    }

    @PostMapping("/token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        authService.changePassword(request.get("oldPassword"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        authService.deleteAccount();
        return ResponseEntity.ok(Map.of("message", "账号已注销"));
    }
}
