package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 系统操作日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operation_log")
public class OperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作人用户ID */
    @Column(name = "user_id")
    private UUID userId;

    /** 操作人用户名 */
    @Column(name = "username", length = 200)
    private String username;

    /** 操作类型：CHAT / SESSION / AUTH / CUSTOMER / NOAUTH / SYSTEM */
    @Column(name = "operation_type", length = 50)
    private String operationType;

    /** 请求URL */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /** 请求方式：GET / POST / PUT / DELETE */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /** 请求参数（截断处理，最大4000字符） */
    @Column(name = "request_params", length = 4000)
    private String requestParams;

    /** 客户端IP地址 */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** 响应状态码：200 / 500 等 */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** 执行耗时（毫秒） */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /** 操作描述 */
    @Column(name = "operation_desc", length = 500)
    private String operationDesc;

    /** 异常信息 */
    @Column(name = "error_msg", length = 4000)
    private String errorMsg;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
