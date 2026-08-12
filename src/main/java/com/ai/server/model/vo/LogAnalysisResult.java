package com.ai.server.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 操作日志分析结果 — 结构化分析数据，供前端渲染分析预览卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAnalysisResult {

    /** 日志总数 */
    private int totalLogs;

    /** 涉及用户数 */
    private int uniqueUserCount;

    /** 时间范围描述 */
    private String timeRange;

    /** CRUD 请求方式统计 (GET/POST/PUT/DELETE → 次数) */
    private Map<String, Long> crudStats;

    /** 操作类型统计 (操作类型 → 次数) */
    private Map<String, Long> operationTypeStats;

    /** DELETE 操作总数 */
    private long deleteTotal;

    /** DELETE 成功次数 */
    private long deleteSuccess;

    /** DELETE 失败次数 */
    private long deleteFailure;

    /** DELETE 成功率 */
    private double deleteSuccessRate;

    /** DELETE 操作最多的用户 TOP5 (用户名 → 次数) */
    private Map<String, Long> topDeleteUsers;

    /** 高频操作接口 TOP5 (接口描述 → 次数) */
    private List<TopOperation> topOperations;

    /** 总错误请求数 */
    private long errorCount;

    /** 错误率 */
    private double errorRate;

    /** 平均耗时(ms) */
    private double avgTimeMs;

    /** 最活跃用户 */
    private String mostActiveUser;

    /** 最活跃用户操作次数 */
    private long mostActiveUserCount;

    /** AI 生成的智能洞察列表 */
    private List<LogInsight> insights;

    /**
     * 高频操作接口
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopOperation {
        /** 请求方式 */
        private String method;
        /** 请求URL */
        private String url;
        /** 调用次数 */
        private long count;
    }
}
