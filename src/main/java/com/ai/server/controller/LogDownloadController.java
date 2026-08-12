package com.ai.server.controller;

import com.ai.server.service.RedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;

/**
 * 日志文件下载控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/logs")
@RequiredArgsConstructor
public class LogDownloadController {

    private final RedisService redisService;

    private static final String EXPORT_FILE_PREFIX = "log:export:";
    private static final String REPORT_FILE_PREFIX = "log:report:";

    /**
     * 下载 Word 分析报告文件
     */
    @GetMapping("/download/report/{token}")
    public void downloadReportFile(@PathVariable String token, HttpServletResponse response) {
        String redisKey = REPORT_FILE_PREFIX + token;
        String base64Data = redisService.getCacheObject(redisKey);

        if (base64Data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"code\":404,\"message\":\"下载链接已过期或不存在，请重新生成报告\"}");
            } catch (IOException ignored) {
                log.error("Failed to write response");
            }
            return;
        }

        byte[] fileBytes = Base64.getDecoder().decode(base64Data);
        String fileName = "operation_log_analysis_report.docx";

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setContentLength(fileBytes.length);

        try {
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载分析报告失败: token={}, error={}", token, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据下载令牌下载导出的Excel文件
     */
    @GetMapping("/download/excel/{token}")
    public void downloadLogFile(@PathVariable String token, HttpServletResponse response) {
        String redisKey = EXPORT_FILE_PREFIX + token;
        String base64Data = redisService.getCacheObject(redisKey);

        if (base64Data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"code\":404,\"message\":\"下载链接已过期或不存在，请重新导出\"}");
            } catch (IOException ignored) {
                log.error("Failed to write response");
            }
            return;
        }

        byte[] fileBytes = Base64.getDecoder().decode(base64Data);
        String fileName = "operation_log_export.xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setContentLength(fileBytes.length);

        try {
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载日志文件失败: token={}, error={}", token, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
