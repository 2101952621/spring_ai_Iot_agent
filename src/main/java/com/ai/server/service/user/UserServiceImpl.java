package com.ai.server.service.user;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.server.common.exception.BusinessException;
import com.ai.server.model.entity.UserEntity;
import com.ai.server.model.vo.UserVO;
import com.ai.server.repository.UserRepository;
import com.ai.server.security.auth.RegisterRequest;
import com.ai.server.security.auth.ResetPasswordRequest;
import com.ai.server.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public void sendRegisterMail(String email) {
        if (StrUtil.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("该邮箱已被注册");
        }

        String activateToken = IdUtil.fastSimpleUUID();
        UserEntity user = UserEntity.builder()
                .email(email)
                .password("")
                .enabled(true)
                .activated(false)
                .activateToken(activateToken)
                .build();
        userRepository.save(user);

        emailService.sendRegisterMail(email, activateToken);
    }

    @Override
    public UserVO registerByEmail(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已被注册");
        }

        String activateToken = IdUtil.fastSimpleUUID();
        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .enabled(true)
                .activated(true)  //测试场景默认直接激活,真实场景需要邮箱验证激活
                .activateToken(activateToken)
                .build();
        userRepository.save(user);
        //TODO 发送激活邮件
//        emailService.sendRegisterMail(request.getEmail(), activateToken);

        return UserVO.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .activated(user.getActivated())
                .build();
    }

    @Override
    public String checkActivateToken(String activateToken) {
        if (StrUtil.isBlank(activateToken)) {
            throw new BusinessException("无效的激活链接");
        }

        UserEntity user = userRepository.findByActivateToken(activateToken)
                .orElseThrow(() -> new BusinessException("无效的激活链接"));

        return user.getEmail();
    }

    @Override
    public void activateUser(String activateToken) {
        if (StrUtil.isBlank(activateToken)) {
            throw new BusinessException("激活令牌不能为空");
        }

        UserEntity user = userRepository.findByActivateToken(activateToken)
                .orElseThrow(() -> new BusinessException("无效的激活链接"));

        user.setActivated(true);
        user.setActivateToken(null);
        userRepository.save(user);
    }

    @Override
    public void sendResetPasswordMail(String email) {
        if (StrUtil.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("该邮箱未注册"));

        String resetToken = IdUtil.fastSimpleUUID();
        user.setResetToken(resetToken);
        user.setResetTokenExpireTime(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendResetPasswordMail(email, resetToken);
    }

    @Override
    public String checkResetToken(String resetToken) {
        if (StrUtil.isBlank(resetToken)) {
            throw new BusinessException("无效的重置链接");
        }

        UserEntity user = userRepository.findByResetToken(resetToken)
                .orElseThrow(() -> new BusinessException("无效的重置链接"));

        if (user.getResetTokenExpireTime() != null &&
                user.getResetTokenExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置链接已过期，请重新申请");
        }

        return user.getEmail();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (StrUtil.isBlank(request.getResetToken())) {
            throw new BusinessException("重置令牌不能为空");
        }

        UserEntity user = userRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new BusinessException("无效的重置链接"));

        if (user.getResetTokenExpireTime() != null &&
                user.getResetTokenExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置链接已过期，请重新申请");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpireTime(null);
        userRepository.save(user);
    }
}
