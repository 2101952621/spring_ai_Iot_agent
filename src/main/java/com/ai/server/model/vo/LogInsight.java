package com.ai.server.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogInsight {

    /**
     * 洞察级别
     */
    public enum Level {
        /** 危险 — 需要立即关注 */
        DANGER,
        /** 警告 — 存在潜在风险 */
        WARNING,
        /** 提示 — 正常统计信息 */
        INFO
    }

    /**
     * 洞察类型
     */
    public enum Type {
        /** 频繁删除操作 */
        FREQUENT_DELETE,
        /** 高频接口调用 */
        HIGH_FREQUENCY_API,
        /** 高错误率 */
        HIGH_ERROR_RATE,
        /** 慢请求 */
        SLOW_REQUEST,
        /** 异常时段操作 */
        ABNORMAL_TIME,
        /** 最活跃用户 */
        ACTIVE_USER,
        /** DELETE 失败率高 */
        DELETE_FAILURE,
        /** 常规统计 */
        GENERAL
    }

    /** 洞察级别 */
    private Level level;

    /** 洞察类型 */
    private Type type;

    /** 标题（简短描述） */
    private String title;

    /** 详细描述 */
    private String description;

    /** 运维建议 */
    private String suggestion;
}
