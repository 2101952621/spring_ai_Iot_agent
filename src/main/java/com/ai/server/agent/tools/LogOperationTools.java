package com.ai.server.agent.tools;

import cn.hutool.core.util.IdUtil;
import com.ai.server.config.ToolResultUtils;
import com.ai.server.repository.UserRepository;
import com.ai.server.service.OperationLogService;
import com.ai.server.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
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

    private final OperationLogService operationLogService;
    private final RedisService redisService;
    private final UserRepository userRepository;

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
            String downloadUrl = "/api/ai/logs/download/" + downloadToken;

            Map<String, Object> exportInfo = new LinkedHashMap<>();
            exportInfo.put("fileName", fileName);
            exportInfo.put("fileSize", excelBytes.length);
            exportInfo.put("downloadUrl", downloadUrl);
            exportInfo.put("downloadToken", downloadToken);
            exportInfo.put("cardType", "LOG_EXPORT");

            // 存入 ToolResultUtils，供前端 CARD 事件渲染
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
            log.error("非法参数: userId={}, error={}", targetUserId, e.getMessage());
            return "导出失败：用户ID格式不正确，请提供有效的UUID格式。";
        } catch (Exception e) {
            log.error("导出日志失败: userId={}, startDate={}, endDate={}, error={}",
                    targetUserId, startDate, endDate, e.getMessage());
            return "导出日志失败：" + e.getMessage() + "。请稍后重试。";
        }
    }

    private String buildFileName(UUID userId, String startDate, String endDate) {
        StringBuilder sb = new StringBuilder("operation_log");
        if (userId != null) {
            sb.append("_user_").append(userId.toString().substring(0, 8));
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
