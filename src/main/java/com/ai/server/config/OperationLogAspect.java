package com.ai.server.config;

import com.ai.server.model.entity.OperationLogEntity;
import com.ai.server.security.SecurityUser;
import com.ai.server.security.SecurityUtils;
import com.ai.server.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 操作日志AOP切面 — 拦截所有 Controller 方法并记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final HttpServletRequest request;

    /**
     * 拦截所有 controller 包下的方法
     */
    @Around("execution(* com.ai.server.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法信息
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        String ipAddress = getClientIp(request);

        // 获取请求参数
        String requestParams = extractRequestParams(joinPoint);

        // 获取当前用户
        String username = "anonymous";
        String userIdStr = null;
        Optional<SecurityUser> userOptional = SecurityUtils.currentUserOptional();
        if (userOptional.isPresent()) {
            SecurityUser user = userOptional.get();
            username = user.getEmail() != null ? user.getEmail() : user.getUsername();
            userIdStr = user.getUuidId() != null ? user.getUuidId().toString() : null;
        }

        // 判断操作类型
        String operationType = determineOperationType(requestUrl);
        String operationDesc = className + "." + methodName;

        Object result;
        int responseStatus = 200;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            responseStatus = 500;
            errorMsg = t.getMessage();
            long executionTime = System.currentTimeMillis() - startTime;
            saveOperationLog(username, userIdStr, operationType, requestUrl, requestMethod,
                    requestParams, ipAddress, responseStatus, executionTime, operationDesc, errorMsg);
            throw t;
        }

        long executionTime = System.currentTimeMillis() - startTime;
        saveOperationLog(username, userIdStr, operationType, requestUrl, requestMethod,
                requestParams, ipAddress, responseStatus, executionTime, operationDesc, errorMsg);

        return result;
    }

    private void saveOperationLog(String username, String userIdStr, String operationType,
                                   String requestUrl, String requestMethod, String requestParams,
                                   String ipAddress, Integer responseStatus, Long executionTime,
                                   String operationDesc, String errorMsg) {
        try {
            OperationLogEntity logEntity = OperationLogEntity.builder()
                    .userId(userIdStr != null ? java.util.UUID.fromString(userIdStr) : null)
                    .username(username)
                    .operationType(operationType)
                    .requestUrl(requestUrl)
                    .requestMethod(requestMethod)
                    .requestParams(requestParams != null && requestParams.length() > 4000
                            ? requestParams.substring(0, 4000) : requestParams)
                    .ipAddress(ipAddress)
                    .responseStatus(responseStatus)
                    .executionTimeMs(executionTime)
                    .operationDesc(operationDesc)
                    .errorMsg(errorMsg != null && errorMsg.length() > 4000
                            ? errorMsg.substring(0, 4000) : errorMsg)
                    .createTime(LocalDateTime.now())
                    .build();
            operationLogService.save(logEntity);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    /**
     * 根据请求URL判断操作类型
     */
    private String determineOperationType(String requestUrl) {
        if (requestUrl == null) return "UNKNOWN";
        if (requestUrl.contains("/api/ai/chat") || requestUrl.contains("/api/ai/stop")) return "CHAT";
        if (requestUrl.contains("/api/ai/session") || requestUrl.contains("/api/ai/history")
                || requestUrl.contains("/api/ai/search")) return "SESSION";
        if (requestUrl.contains("/api/auth")) return "AUTH";
        if (requestUrl.contains("/api/customer")) return "CUSTOMER";
        if (requestUrl.contains("/api/noauth")) return "NOAUTH";
        if (requestUrl.contains("/api/ai/hot") || requestUrl.contains("/api/ai/functions")
                || requestUrl.contains("/api/ai/versions")) return "AI";
        if (requestUrl.contains("/api/ai/logs")) return "LOG";
        return "SYSTEM";
    }

    /**
     * 提取请求参数（截断处理）
     */
    private String extractRequestParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            String className = arg.getClass().getName();
            if (className.startsWith("jakarta.servlet") || className.startsWith("org.springframework")) {
                continue;
            }
            if (i > 0) sb.append(", ");
            try {
                sb.append(arg);
            } catch (Exception e) {
                sb.append("[...]");
            }
            if (sb.length() > 2000) {
                sb.append("...(truncated)");
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
