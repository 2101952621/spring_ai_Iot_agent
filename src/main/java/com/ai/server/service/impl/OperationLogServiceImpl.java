package com.ai.server.service.impl;

import com.ai.server.model.entity.OperationLogEntity;
import com.ai.server.repository.OperationLogRepository;
import com.ai.server.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 系统操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogRepository operationLogRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void save(OperationLogEntity log) {
        operationLogRepository.save(log);
    }

    @Override
    public List<OperationLogEntity> findByUserId(UUID userId) {
        return operationLogRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Override
    public List<OperationLogEntity> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        return operationLogRepository.findByCreateTimeBetween(start, end);
    }

    @Override
    public List<OperationLogEntity> findByUserIdAndTimeRange(UUID userId, LocalDateTime start, LocalDateTime end) {
        return operationLogRepository.findByUserIdAndCreateTimeBetween(userId, start, end);
    }

    @Override
    public int deleteByTimeRange(LocalDateTime start, LocalDateTime end) {
        int count = operationLogRepository.deleteByCreateTimeBetween(start, end);
        log.info("清理操作日志: 时间范围=[{} ~ {}], 删除条数={}", start, end, count);
        return count;
    }

    @Override
    public List<OperationLogEntity> findByOperationType(String operationType) {
        return operationLogRepository.findByOperationTypeOrderByCreateTimeDesc(operationType);
    }

    @Override
    public byte[] exportToExcel(UUID userId, LocalDateTime start, LocalDateTime end) {
        List<OperationLogEntity> logs;
        if (userId != null && start != null && end != null) {
            logs = operationLogRepository.findByUserIdAndCreateTimeBetween(userId, start, end);
        } else if (userId != null) {
            logs = operationLogRepository.findByUserIdOrderByCreateTimeDesc(userId);
        } else if (start != null && end != null) {
            logs = operationLogRepository.findByCreateTimeBetween(start, end);
        } else {
            logs = operationLogRepository.findAll();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");

            // 设置列宽
            int[] widths = {8, 40, 20, 20, 40, 10, 40, 20, 12, 12, 30, 22};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 表头
            String[] headers = {"序号", "用户ID", "用户名", "操作类型", "请求URL", "请求方式",
                    "请求参数", "IP地址", "响应状态", "耗时(ms)", "操作描述", "操作时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            for (int i = 0; i < logs.size(); i++) {
                OperationLogEntity logEntity = logs.get(i);
                Row row = sheet.createRow(i + 1);
                fillCell(row, 0, i + 1, dataStyle);
                fillCell(row, 1, logEntity.getUserId() != null ? logEntity.getUserId().toString() : "", dataStyle);
                fillCell(row, 2, logEntity.getUsername(), dataStyle);
                fillCell(row, 3, logEntity.getOperationType(), dataStyle);
                fillCell(row, 4, logEntity.getRequestUrl(), dataStyle);
                fillCell(row, 5, logEntity.getRequestMethod(), dataStyle);
                fillCell(row, 6, truncate(logEntity.getRequestParams(), 500), dataStyle);
                fillCell(row, 7, logEntity.getIpAddress(), dataStyle);
                fillCell(row, 8, logEntity.getResponseStatus() != null ? String.valueOf(logEntity.getResponseStatus()) : "", dataStyle);
                fillCell(row, 9, logEntity.getExecutionTimeMs() != null ? String.valueOf(logEntity.getExecutionTimeMs()) : "", dataStyle);
                fillCell(row, 10, logEntity.getOperationDesc(), dataStyle);
                fillCell(row, 11, logEntity.getCreateTime() != null ? logEntity.getCreateTime().format(FORMATTER) : "", dataStyle);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("导出操作日志Excel失败", e);
            throw new RuntimeException("导出操作日志失败: " + e.getMessage(), e);
        }
    }

    private void fillCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue("");
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}
