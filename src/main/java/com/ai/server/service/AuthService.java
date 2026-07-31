package com.ai.server.service;

import com.ai.server.model.vo.UserVO;
import com.ai.server.security.auth.AuthResponse;
import com.ai.server.security.auth.LoginRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);

    /**
     * 刷新Token
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser();

    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * 注销账号
     */
    void deleteAccount();
}
