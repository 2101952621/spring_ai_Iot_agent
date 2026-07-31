package com.ai.server.service;

import cn.hutool.core.util.StrUtil;
import com.ai.server.model.vo.UserVO;
import com.ai.server.common.exception.BusinessException;
import com.ai.server.model.entity.UserEntity;
import com.ai.server.repository.UserRepository;
import com.ai.server.security.JwtTokenProvider;
import com.ai.server.security.SecurityUser;
import com.ai.server.security.SecurityUtils;
import com.ai.server.security.UserDetailsServiceImpl;
import com.ai.server.security.auth.AuthResponse;
import com.ai.server.security.auth.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new BusinessException("邮箱或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
        }

        if (!user.getEnabled()) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        if (!user.getActivated()) {
            throw new BusinessException("账号未激活，请先激活账号");
        }

        SecurityUser securityUser = userDetailsService.toSecurityUser(user);

        String accessToken = jwtTokenProvider.generateAccessToken(securityUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(securityUser);

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        return AuthResponse.of(accessToken, refreshToken, jwtExpiration);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken) || !refreshToken.startsWith("Bearer ")) {
            throw new BusinessException("无效的刷新令牌");
        }
        String token = refreshToken.substring(7);
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new BusinessException("刷新令牌已过期，请重新登录");
        }

        String username = jwtTokenProvider.extractUsername(token);
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        SecurityUser securityUser = userDetailsService.toSecurityUser(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(securityUser);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(securityUser);

        return AuthResponse.of(newAccessToken, newRefreshToken, jwtExpiration);
    }

    @Override
    public UserVO getCurrentUser() {
        SecurityUser currentUser = SecurityUtils.currentUser();
        return UserVO.builder()
                .id(currentUser.getUuidId().toString())
                .email(currentUser.getEmail())
                .firstName(currentUser.getFirstName())
                .lastName(currentUser.getLastName())
                .phone(currentUser.getPhone())
                .enabled(currentUser.isEnabled())
                .activated(currentUser.isActivated())
                .build();
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        SecurityUser currentUser = SecurityUtils.currentUser();

        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            throw new BusinessException("旧密码和新密码不能为空");
        }

        UserEntity user = userRepository.findById(currentUser.getUuidId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void deleteAccount() {
        SecurityUser currentUser = SecurityUtils.currentUser();
        userRepository.deleteById(currentUser.getUuidId());
    }
}
