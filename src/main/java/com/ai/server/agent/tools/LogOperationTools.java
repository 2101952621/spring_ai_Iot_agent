package com.ai.server.agent.tools;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.server.config.ToolResultUtils;
import com.ai.server.repository.UserRepository;
import com.ai.server.service.LogAnalysisReportService;
import com.ai.server.service.OperationLogService;
import com.ai.server.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.ai.server.model.entity.OperationLogEntity;
import com.ai.server.model.vo.LogAnalysisResult;
import com.ai.server.model.vo.LogInsight;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 系统日志操作工具 — 供 SystemControlAgent 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogOperationTools {

    public static final String TOOL_RESULT_KEY_EXPORT = "DownloadExport";

    /** 分析报告导出的 ToolResult key */
    public static final String TOOL_RESULT_KEY_REPORT = "DownloadReport";

    /** 分析摘要预览的 ToolResult key */
    public static final String TOOL_RESULT_KEY_ANALYSIS = "AnalysisPreview";

    /** Word 分析报告文件 Redis 前缀 */
    private static final String REPORT_FILE_PREFIX = "log:report:";

    private final OperationLogService operationLogService;
    private final RedisService redisService;
    private final UserRepository userRepository;
    private final LogAnalysisReportService logAnalysisReportService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String EXPORT_FILE_PREFIX = "log:export:";

    /**
     * 清理指定时间范围内的操作日志
     *
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate   结束日期，格式：yyyy-MM-dd
     * @return 清理结果描述
     */
    @Tool(description = "清理指定时间范围内的系统操作日志。需要提供开始日期和结束日期，格式为yyyy-MM-dd")
    public String cleanLogsByTimeRange(
            @ToolParam(description = "开始日期，格式yyyy-MM-dd，例如2026-07-01") String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd，例如2026-07-31") String endDate) {
        try {
            //TODO: 实际应用场景可根据用户角色权限去做限制
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            LocalDateTime startTime = start.atStartOfDay();
            LocalDateTime endTime = end.atTime(LocalTime.MAX);

            int deleted = operationLogService.deleteByTimeRange(startTime, endTime);
            log.info("日志清理完成: startDate={}, endDate={}, deletedCount={}", startDate, endDate, deleted);

            if (deleted > 0) {
                return String.format("成功清理了 %d 条操作日志（时间范围：%s ~ %s）", deleted, startDate, endDate);
            } else {
                return String.format("时间范围 %s ~ %s 内没有需要清理的操作日志", startDate, endDate);
            }
        } catch (Exception e) {
            log.error("清理日志失败: startDate={}, endDate={}, error={}", startDate, endDate, e.getMessage());
            return "清理日志失败：" + e.getMessage() + "。请检查日期格式是否正确（应为yyyy-MM-dd）";
        }
    }

    /**
     * 导出指定用户的操作日志为Excel文件
     *
     * @param targetUserId 目标用户ID（UUID格式），可传空字符串导出全部用户日志
     * @param startDate    开始日期（可选），格式yyyy-MM-dd
     * @param endDate      结束日期（可选），格式yyyy-MM-dd
     * @return 导出结果描述（含下载令牌）
     */
    @Tool(description = "导出指定用户的操作日志为Excel文件并返回下载地址。" +
            "当用户说'导出我的操作日志'时不传userId或传空；当说'导出某个用户的操作日志'时传入该用户的userId。" +
            "startDate和endDate为可选参数，不传则导出全部日志")
    public String exportUserLogs(
            @ToolParam(description = "要导出的用户ID（UUID格式），不传或传空字符串表示导出全部用户", required = false) String targetUserId,
            @ToolParam(description = "开始日期（可选），格式yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "结束日期（可选），格式yyyy-MM-dd", required = false) String endDate,
            ToolContext toolContext) {

        try {
            //TODO: 实际应用场景可根据用户角色权限去做限制,最高管理员可导出系统所有用户的日志
            UUID userId = (targetUserId != null && !targetUserId.isBlank())
                    ? UUID.fromString(targetUserId) : null;
            if (userId != null && !userRepository.existsById(userId)) {
                log.warn("导出日志失败: 用户ID {} 在系统中不存在", targetUserId);
                return "导出失败：系统中不存在用户ID为 " + targetUserId + " 的用户，请检查用户ID是否正确。";
            }

            LocalDateTime startTime = null;
            LocalDateTime endTime = null;

            if (startDate != null && !startDate.isBlank()) {
                startTime = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
            }
            if (endDate != null && !endDate.isBlank()) {
                endTime = LocalDate.parse(endDate, DATE_FORMATTER).atTime(LocalTime.MAX);
            }

            byte[] excelBytes = operationLogService.exportToExcel(userId, startTime, endTime);

            // 生成下载令牌并存入Redis
            String downloadToken = IdUtil.fastSimpleUUID();
            String redisKey = EXPORT_FILE_PREFIX + downloadToken;
            String base64Data = Base64.getEncoder().encodeToString(excelBytes);
            redisService.setCacheObject(redisKey, base64Data, 10L, TimeUnit.MINUTES);

            // 构建下载信息
            String fileName = buildFileName(userId, startDate, endDate);
            String downloadUrl = "/api/ai/logs/download/excel/" + downloadToken;

            Map<String, Object> exportInfo = new LinkedHashMap<>();
            exportInfo.put("fileName", fileName);
            exportInfo.put("fileSize", excelBytes.length);
            exportInfo.put("downloadUrl", downloadUrl);
            exportInfo.put("downloadToken", downloadToken);
            exportInfo.put("cardType", "LOG_EXPORT");

            String requestId = resolveRequestId(toolContext);
            if (requestId != null) {
                ToolResultUtils.put(requestId, TOOL_RESULT_KEY_EXPORT, exportInfo);
            }

            log.info("日志导出成功: userId={}, startDate={}, endDate={}, fileSize={}, token={}",
                    userId, startDate, endDate, excelBytes.length, downloadToken);

            String userDesc = userId != null ? "用户(" + userId + ")" : "全部用户";
            String timeDesc = (startDate != null || endDate != null)
                    ? "（时间范围：" + (startDate != null ? startDate : "不限") + " ~ " + (endDate != null ? endDate : "不限") + "）"
                    : "";
            return String.format("已成功导出%s的操作日志%s，文件名：%s，共 %.1f KB。" +
                            "请告知用户点击下方按钮下载Excel文件。",
                    userDesc, timeDesc, fileName, excelBytes.length / 1024.0);
        } catch (IllegalArgumentException e) {
            return "导出失败：用户ID格式不正确，请提供有效的UUID格式。";
        } catch (Exception e) {
            log.error("导出日志失败: userId={}, startDate={}, endDate={}, error={}",
                    targetUserId, startDate, endDate, e.getMessage());
            return "导出日志失败：" + e.getMessage() + "。请稍后重试。";
        }
    }

    /**
     * 生成操作日志 Word 可视化分析报告
     *
     * @param targetUserId 目标用户ID（UUID格式），可传空字符串分析全部用户
     * @param startDate    开始日期（可选），格式 yyyy-MM-dd
     * @param endDate      结束日期（可选），格式 yyyy-MM-dd
     * @return 分析结果摘要（含下载链接）
     */
    @Tool(description = "生成用户操作日志的Word分析报告。报告中包含增删改查(CRUD)接口的调用次数统计、占比、" +
            "请求方式分布饼图、操作类型分布饼图、DELETE删除接口调用的详细指标（成功率、用户分布、时间趋势折线图）、" +
            "高频操作TOP10以及AI智能分析总结。" +
            "当用户在导出日志后确认需要分析报告，或直接说'分析我的操作日志'、'生成分析报告'、'帮我分析操作记录'时调用此工具。" +
            "targetUserId 不传或传空字符串表示分析全部用户；startDate、endDate 为可选参数")
    public String generateLogAnalysisReport(
            @ToolParam(description = "要分析的用户ID（UUID格式），不传或传空字符串表示分析全部用户", required = false) String targetUserId,
            @ToolParam(description = "开始日期（可选），格式 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "结束日期（可选），格式 yyyy-MM-dd", required = false) String endDate,
            ToolContext toolContext) {

        try {
            //TODO: 实际应用场景可根据用户角色权限去做限制,最高管理员可导出系统所有用户的日志
            UUID userId = (targetUserId != null && !targetUserId.isBlank())
                    ? UUID.fromString(targetUserId) : null;
            if (userId != null && !userRepository.existsById(userId)) {
                log.warn("生成分析报告失败: 用户ID {} 在系统中不存在", targetUserId);
                return "分析失败：系统中不存在用户ID为 " + targetUserId + " 的用户，请检查用户ID是否正确。";
            }

            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            if (startDate != null && !startDate.isBlank()) {
                startTime = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
            }
            if (endDate != null && !endDate.isBlank()) {
                endTime = LocalDate.parse(endDate, DATE_FORMATTER).atTime(LocalTime.MAX);
            }

            // 查询日志数据
            List<OperationLogEntity> logs;
            if (userId != null && startTime != null && endTime != null) {
                logs = operationLogService.findByUserIdAndTimeRange(userId, startTime, endTime);
            } else if (userId != null) {
                logs = operationLogService.findByUserId(userId);
            } else if (startTime != null && endTime != null) {
                logs = operationLogService.findByTimeRange(startTime, endTime);
            } else {
                logs = operationLogService.findByTimeRange(
                        LocalDate.now().minusMonths(1).atStartOfDay(), LocalDateTime.now());
            }

            // 解析用户名描述
            String username = null;
            if (userId != null) {
                username = userRepository.findById(userId)
                        .map(u -> {
                            String full = StrUtil.isBlank(u.getFirstName()) && StrUtil.isBlank(u.getLastName())
                                    ? u.getEmail() : (u.getFirstName() + " " + u.getLastName());
                            return full.trim();
                        }).orElse(targetUserId);
            }

            // 构建时间范围描述
            String timeRangeDesc = (startDate != null || endDate != null)
                    ? (startDate != null ? startDate : "不限") + " ~ " + (endDate != null ? endDate : "不限")
                    : "最近30天";

            // 生成 Word 报告
            byte[] reportBytes = logAnalysisReportService.generateAnalysisReport(logs, username, timeRangeDesc);

            // 结构化分析（生成智能洞察供前端预览和 AI 参考）
            LogAnalysisResult analysisResult = logAnalysisReportService.analyze(logs, timeRangeDesc);

            // 生成下载令牌并存入Redis
            String downloadToken = IdUtil.fastSimpleUUID();
            String redisKey = REPORT_FILE_PREFIX + downloadToken;
            String base64Data = Base64.getEncoder().encodeToString(reportBytes);
            redisService.setCacheObject(redisKey, base64Data, 30L, TimeUnit.MINUTES);

            // 构建下载信息
            String fileName = buildReportFileName(userId, startDate, endDate);
            String downloadUrl = "/api/ai/logs/download/report/" + downloadToken;

            Map<String, Object> reportInfo = new LinkedHashMap<>();
            reportInfo.put("fileName", fileName);
            reportInfo.put("fileSize", reportBytes.length);
            reportInfo.put("downloadUrl", downloadUrl);
            reportInfo.put("downloadToken", downloadToken);
            reportInfo.put("cardType", "REPORT_EXPORT");
            Map<String, Object> analysisPreview = new LinkedHashMap<>();
            analysisPreview.put("cardType", "ANALYSIS_PREVIEW");
            analysisPreview.put("totalLogs", analysisResult.getTotalLogs());
            analysisPreview.put("uniqueUserCount", analysisResult.getUniqueUserCount());
            analysisPreview.put("timeRange", analysisResult.getTimeRange());
            analysisPreview.put("crudStats", analysisResult.getCrudStats());
            analysisPreview.put("deleteTotal", analysisResult.getDeleteTotal());
            analysisPreview.put("deleteSuccessRate", analysisResult.getDeleteSuccessRate());
            analysisPreview.put("errorCount", analysisResult.getErrorCount());
            analysisPreview.put("errorRate", analysisResult.getErrorRate());
            analysisPreview.put("avgTimeMs", analysisResult.getAvgTimeMs());
            analysisPreview.put("mostActiveUser", analysisResult.getMostActiveUser());
            analysisPreview.put("mostActiveUserCount", analysisResult.getMostActiveUserCount());
            analysisPreview.put("topDeleteUsers", analysisResult.getTopDeleteUsers());
            analysisPreview.put("topOperations", analysisResult.getTopOperations());
            analysisPreview.put("insights", analysisResult.getInsights());
            String requestId = resolveRequestId(toolContext);
            if (requestId != null) {
                ToolResultUtils.put(requestId, TOOL_RESULT_KEY_REPORT, reportInfo);
                ToolResultUtils.put(requestId, TOOL_RESULT_KEY_ANALYSIS, analysisPreview);
            }
            return buildAiResponseText(username, analysisResult, fileName);
        } catch (IllegalArgumentException e) {
            return "分析失败：用户ID格式不正确，请提供有效的UUID格式。";
        } catch (Exception e) {
            return "生成分析报告失败：" + e.getMessage() + "。请稍后重试。";
        }
    }

    private String buildAiResponseText(String username, LogAnalysisResult result, String fileName) {
        String userDesc = username != null ? "用户「" + username + "」" : "全部用户";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("已成功为%s生成操作日志分析报告（文件名：%s，共分析 %d 条日志）。\n",
                userDesc, fileName, result.getTotalLogs()));

        sb.append("\n## 统计概览\n");
        sb.append(String.format("- 涉及用户：%d 人\n", result.getUniqueUserCount()));
        sb.append(String.format("- DELETE 操作：%d 次（成功率 %.1f%%）\n", result.getDeleteTotal(), result.getDeleteSuccessRate()));
        sb.append(String.format("- 错误请求：%d 次（错误率 %.1f%%）\n", result.getErrorCount(), result.getErrorRate()));
        sb.append(String.format("- 平均耗时：%.0f ms\n", result.getAvgTimeMs()));
        if (result.getMostActiveUser() != null) {
            sb.append(String.format("- 最活跃用户：%s（%d 次操作）\n", result.getMostActiveUser(), result.getMostActiveUserCount()));
        }

        if (result.getCrudStats() != null && !result.getCrudStats().isEmpty()) {
            sb.append("\n## CRUD 请求方式分布\n");
            result.getCrudStats().forEach((k, v) ->
                    sb.append(String.format("- %s: %d 次\n", k, v)));
        }

        if (result.getTopOperations() != null && !result.getTopOperations().isEmpty()) {
            sb.append("\n## 高频操作接口 TOP5\n");
            for (LogAnalysisResult.TopOperation op : result.getTopOperations()) {
                sb.append(String.format("- [%s] %s → %d 次\n", op.getMethod(), truncateUrl(op.getUrl(), 70), op.getCount()));
            }
        }

        if (result.getTopDeleteUsers() != null && !result.getTopDeleteUsers().isEmpty()) {
            sb.append("\n## DELETE 操作用户分布 TOP5\n");
            result.getTopDeleteUsers().forEach((k, v) ->
                    sb.append(String.format("- %s: %d 次\n", k, v)));
        }

        if (result.getInsights() != null && !result.getInsights().isEmpty()) {
            sb.append("\n## AI 智能洞察与运维建议（重要 — 请基于以下内容向用户总结）\n");
            for (LogInsight insight : result.getInsights()) {
                sb.append(String.format("- 【%s】%s\n", insight.getLevel(), insight.getTitle()));
                sb.append(String.format("  情况：%s\n", insight.getDescription()));
                sb.append(String.format("  建议：%s\n", insight.getSuggestion()));
            }
        }

        sb.append("\n## 输出要求\n");
        sb.append("请基于以上分析数据，为用户生成一段简洁的运维总结，要点如下：\n");
        sb.append("1. 概述分析范围和总体情况\n");
        sb.append("2. 重点提示危险和警告级别的洞察（如频繁删除、高频接口、高错误率等）\n");
        sb.append("3. 给出具体可执行的运维建议\n");
        sb.append("4. 提示用户可以点击下方按钮下载完整的 Word 分析报告（含图表）\n");
        sb.append("注意：下方会自动显示分析预览卡片和下载按钮，你不需要重复输出统计数据。\n");
        return sb.toString();
    }

    private String truncateUrl(String url, int maxLen) {
        if (url == null) return "-";
        return url.length() > maxLen ? url.substring(0, maxLen) + "..." : url;
    }

    private String buildReportFileName(UUID userId, String startDate, String endDate) {
        StringBuilder sb = new StringBuilder("operation_log_analysis_report");
        if (userId != null) {
            sb.append("_user_").append(userId.toString(), 0, 8);
        }
        if (startDate != null) {
            sb.append("_").append(startDate);
        }
        if (endDate != null) {
            sb.append("_").append(endDate);
        }
        sb.append(".docx");
        return sb.toString();
    }

    private String buildFileName(UUID userId, String startDate, String endDate) {
        StringBuilder sb = new StringBuilder("operation_log");
        if (userId != null) {
            sb.append("_user_").append(userId.toString(), 0, 8);
        }
        if (startDate != null) {
            sb.append("_").append(startDate);
        }
        if (endDate != null) {
            sb.append("_").append(endDate);
        }
        sb.append(".xlsx");
        return sb.toString();
    }

    private String resolveRequestId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) return null;
        Object v = toolContext.getContext().get("requestId");
        return v != null ? v.toString() : null;
    }
}
