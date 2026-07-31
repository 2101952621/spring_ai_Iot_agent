package com.ai.server.service.user;

import com.ai.server.model.vo.UserVO;
import com.ai.server.security.auth.RegisterRequest;
import com.ai.server.security.auth.ResetPasswordRequest;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 发送注册邮件
     */
    void sendRegisterMail(String email);

    /**
     * 邮箱注册
     */
    UserVO registerByEmail(RegisterRequest request);

    /**
     * 检查激活Token，返回邮箱
     */
    String checkActivateToken(String activateToken);

    /**
     * 激活用户
     */
    void activateUser(String activateToken);

    /**
     * 发送重置密码邮件
     */
    void sendResetPasswordMail(String email);

    /**
     * 检查重置Token，返回邮箱
     */
    String checkResetToken(String resetToken);

    /**
     * 重置密码
     */
    void resetPassword(ResetPasswordRequest request);
}
