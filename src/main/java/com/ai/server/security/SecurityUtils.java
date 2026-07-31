package com.ai.server.security;

import com.ai.server.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;


public final class SecurityUtils {

    private SecurityUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前 SecurityUser
     * @throws BusinessException(401) 未登录时抛出
     */
    public static SecurityUser currentUser() {
        return currentUserOptional()
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
    }

    /**
     * 获取当前登录用户（Optional 包装）
     *
     * @return Optional 包装的 SecurityUser
     */
    public static Optional<SecurityUser> currentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * 判断当前请求是否已认证
     */
    public static boolean isAuthenticated() {
        return currentUserOptional().isPresent();
    }
}
