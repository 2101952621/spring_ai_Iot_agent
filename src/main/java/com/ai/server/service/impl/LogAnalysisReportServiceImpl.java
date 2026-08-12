package com.ai.server.service.impl;

import com.ai.server.config.LogAnalysisChartGenerator;
import com.ai.server.model.entity.OperationLogEntity;
import com.ai.server.model.vo.LogAnalysisResult;
import com.ai.server.model.vo.LogAnalysisResult.TopOperation;
import com.ai.server.model.vo.LogInsight;
import com.ai.server.model.vo.LogInsight.Level;
import com.ai.server.model.vo.LogInsight.Type;
import com.ai.server.service.LogAnalysisReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 操作日志分析报告生成服务实现
 */
@Slf4j
@Service
public class LogAnalysisReportServiceImpl implements LogAnalysisReportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 增删改查请求方式分类
     */
    private static final List<String> CRUD_METHODS = List.of("GET", "POST", "PUT", "DELETE");

    @Override
    public byte[] generateAnalysisReport(List<OperationLogEntity> logs, String username, String timeRange) {
        if (logs == null || logs.isEmpty()) {
            return generateEmptyReport(username, timeRange);
        }

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            // ============ 1. 报告标题与基本信息 ============
            buildReportHeader(doc, logs, username, timeRange);

            // ============ 2. CRUD 接口调用统计 ============
            Map<String, Long> crudStats = buildCrudStats(logs);
            buildSectionTitle(doc, "一、CRUD 接口调用统计");
            buildStatsTable(doc, new String[]{"请求方式", "调用次数", "占比"}, crudStats, logs.size());
            embedImage(doc, LogAnalysisChartGenerator.createPieChart("请求方式分布饼图", crudStats));

            // ============ 3. 操作类型分布 ============
            Map<String, Long> typeStats = buildOperationTypeStats(logs);
            buildSectionTitle(doc, "二、操作类型分布统计");
            buildStatsTable(doc, new String[]{"操作类型", "调用次数", "占比"}, typeStats, logs.size());
            embedImage(doc, LogAnalysisChartGenerator.createPieChart("操作类型分布饼图", typeStats));

            // ============ 4. DELETE 接口调用详细分析（重点） ============
            buildDeleteAnalysisSection(doc, logs);

            // ============ 5. 高频操作 TOP10 ============
            buildSectionTitle(doc, "五、高频操作 TOP10");
            buildTopOperationsTable(doc, logs);

            // ============ 6. AI 智能分析总结 ============
            buildSectionTitle(doc, "六、AI 智能分析总结");
            buildAiSummary(doc, logs, crudStats, typeStats);

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("生成操作日志分析报告失败", e);
            throw new RuntimeException("生成分析报告失败: " + e.getMessage(), e);
        }
    }

    private void buildReportHeader(XWPFDocument doc, List<OperationLogEntity> logs,
                                   String username, String timeRange) {
        // 主标题
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("操作日志分析报告");
        titleRun.setBold(true);
        titleRun.setFontSize(20);
        titleRun.setFontFamily("微软雅黑");
        titleRun.setColor("1F4E79");

        // 统计范围
        LocalDateTime earliest = logs.stream().map(OperationLogEntity::getCreateTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime latest = logs.stream().map(OperationLogEntity::getCreateTime)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);

        long uniqueUsers = logs.stream().map(OperationLogEntity::getUsername)
                .filter(Objects::nonNull).distinct().count();

        Set<String> userSet = logs.stream().map(OperationLogEntity::getUsername)
                .filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));

        Map<String, String> info = new LinkedHashMap<>();
        info.put("报告生成时间", LocalDateTime.now().format(FMT));
        info.put("统计用户", username != null ? username : "全部用户（" + uniqueUsers + " 人）");
        info.put("时间范围", timeRange != null ? timeRange :
                (earliest != null && latest != null ? earliest.format(FMT) + " ~ " + latest.format(FMT) : "全部时间"));
        info.put("日志总数", logs.size() + " 条");
        info.put("最早操作时间", earliest != null ? earliest.format(FMT) : "-");
        info.put("最近操作时间", latest != null ? latest.format(FMT) : "-");
        info.put("涉及用户列表", userSet.isEmpty() ? "-" : String.join("、", userSet));

        buildInfoTable(doc, info);

        // 分隔线
        addSpacer(doc);
    }

    private void buildDeleteAnalysisSection(XWPFDocument doc, List<OperationLogEntity> logs) {
        buildSectionTitle(doc, "三、DELETE 接口调用详细分析（重点操作）");

        List<OperationLogEntity> deleteLogs = logs.stream()
                .filter(l -> "DELETE".equalsIgnoreCase(l.getRequestMethod()))
                .toList();

        XWPFParagraph intro = doc.createParagraph();
        XWPFRun introRun = intro.createRun();
        introRun.setText("DELETE 操作属于高危操作，以下是详细的调用指标分析，便于快速定位关键删除行为。");
        introRun.setFontSize(10);
        introRun.setFontFamily("微软雅黑");

        if (deleteLogs.isEmpty()) {
            XWPFParagraph empty = doc.createParagraph();
            XWPFRun er = empty.createRun();
            er.setText("当前日志范围内没有 DELETE 操作记录。");
            er.setItalic(true);
            er.setFontSize(10);
            er.setFontFamily("微软雅黑");
            return;
        }

        long totalDelete = deleteLogs.size();
        long successDelete = deleteLogs.stream()
                .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                .count();
        long failDelete = totalDelete - successDelete;
        double successRate = totalDelete > 0 ? (successDelete * 100.0 / totalDelete) : 0;
        double avgTimeMs = deleteLogs.stream()
                .map(OperationLogEntity::getExecutionTimeMs)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue).average().orElse(0);
        long maxTimeMs = deleteLogs.stream()
                .map(OperationLogEntity::getExecutionTimeMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(0);

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("DELETE 操作总数", totalDelete + " 次");
        metrics.put("成功次数", successDelete + " 次（成功率 " + String.format("%.1f", successRate) + "%）");
        metrics.put("失败/异常次数", failDelete + " 次（失败率 " + String.format("%.1f", 100 - successRate) + "%）");
        metrics.put("平均耗时", String.format("%.0f", avgTimeMs) + " ms");
        metrics.put("最大耗时", maxTimeMs + " ms");
        metrics.put("占全部操作比例", String.format("%.1f%%", totalDelete * 100.0 / logs.size()));

        buildSectionSubtitle(doc, "3.1 总体指标");
        buildInfoTable(doc, metrics);

        Map<String, Long> deleteUserStats = deleteLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getUsername() != null ? l.getUsername() : "未知用户",
                        LinkedHashMap::new,
                        Collectors.counting()));

        deleteUserStats = sortByValueDesc(deleteUserStats);
        buildSectionSubtitle(doc, "3.2 DELETE 操作用户分布");
        buildStatsTable(doc, new String[]{"用户", "删除次数", "占比"}, deleteUserStats, (int) totalDelete);
        embedImage(doc, LogAnalysisChartGenerator.createBarChart(
                "DELETE 操作用户分布", "用户", "删除次数", "删除次数", topN(deleteUserStats, 10)));

        Map<String, Long> deleteTrend = buildDailyTrend(deleteLogs);
        buildSectionSubtitle(doc, "3.3 DELETE 操作时间趋势（按天）");
        embedImage(doc, LogAnalysisChartGenerator.createLineChart(
                "DELETE 操作每日趋势折线图", "日期", "删除次数", "DELETE", deleteTrend));

        buildSectionSubtitle(doc, "3.4 DELETE 操作明细（最近 10 条）");
        buildDeleteDetailTable(doc, deleteLogs);

        addSpacer(doc);
    }

    private void buildTopOperationsTable(XWPFDocument doc, List<OperationLogEntity> logs) {
        // 按 URL + Method 聚合
        record UrlMethod(String url, String method) {
        }
        Map<UrlMethod, Long> freq = logs.stream()
                .filter(l -> l.getRequestUrl() != null)
                .collect(Collectors.groupingBy(
                        l -> new UrlMethod(l.getRequestUrl(), l.getRequestMethod()),
                        Collectors.counting()));

        List<Map.Entry<UrlMethod, Long>> top = freq.entrySet().stream()
                .sorted(Map.Entry.<UrlMethod, Long>comparingByValue().reversed())
                .limit(10)
                .toList();

        if (top.isEmpty()) {
            addSimpleText(doc, "暂无高频操作数据。");
            return;
        }

        XWPFTable table = doc.createTable(top.size() + 1, 4);
        setTableWidth(table, 9000);
        setHeaderCell(table.getRow(0), 0, "排名");
        setHeaderCell(table.getRow(0), 1, "请求方式");
        setHeaderCell(table.getRow(0), 2, "请求URL");
        setHeaderCell(table.getRow(0), 3, "调用次数");

        AtomicInteger rank = new AtomicInteger(1);
        for (Map.Entry<UrlMethod, Long> e : top) {
            int row = rank.getAndIncrement();
            setDataCell(table.getRow(row), 0, String.valueOf(row));
            setDataCell(table.getRow(row), 1, e.getKey().method != null ? e.getKey().method : "-");
            setDataCell(table.getRow(row), 2, truncate(e.getKey().url, 80));
            setDataCell(table.getRow(row), 3, String.valueOf(e.getValue()));
        }
        addSpacer(doc);
    }

    private void buildAiSummary(XWPFDocument doc, List<OperationLogEntity> logs,
                                Map<String, Long> crudStats, Map<String, Long> typeStats) {
        List<String> findings = new ArrayList<>();
        long total = logs.size();

        // 分析 DELETE 占比
        long deleteCount = crudStats.getOrDefault("DELETE", 0L);
        double deleteRatio = total > 0 ? deleteCount * 100.0 / total : 0;
        if (deleteRatio >= 20) {
            findings.add("⚠ DELETE 操作占比高达 " + String.format("%.1f", deleteRatio)
                    + "%，属于高频删除行为，建议重点审查是否有误删风险。");
        } else if (deleteRatio >= 5) {
            findings.add("DELETE 操作占比 " + String.format("%.1f", deleteRatio)
                    + "%，删除行为处于正常偏高水平，建议关注删除操作的合法性。");
        } else if (deleteCount > 0) {
            findings.add("DELETE 操作占比 " + String.format("%.1f", deleteRatio)
                    + "%，删除行为较少，整体操作风险较低。");
        } else {
            findings.add("当前日志范围内没有 DELETE 操作，操作风险等级低。");
        }

        long errorCount = logs.stream()
                .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 400)
                .count();
        double errorRatio = total > 0 ? errorCount * 100.0 / total : 0;
        if (errorRatio >= 10) {
            findings.add("⚠ 接口错误率高达 " + String.format("%.1f", errorRatio)
                    + "%（" + errorCount + " 次），建议排查系统异常和用户操作失误原因。");
        } else if (errorRatio >= 3) {
            findings.add("接口错误率 " + String.format("%.1f", errorRatio) + "%，建议关注失败请求的具体原因。");
        } else {
            findings.add("接口错误率仅为 " + String.format("%.1f", errorRatio) + "%，系统运行状态良好。");
        }

        Map.Entry<String, Long> topUser = logs.stream()
                .filter(l -> l.getUsername() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getUsername, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (topUser != null) {
            findings.add("最活跃用户为「" + topUser.getKey() + "」，共操作 " + topUser.getValue() + " 次。");
        }

        double avgMs = logs.stream().map(OperationLogEntity::getExecutionTimeMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).average().orElse(0);
        long slowCount = logs.stream().map(OperationLogEntity::getExecutionTimeMs)
                .filter(t -> t != null && t > 1000).count();
        findings.add("接口平均响应耗时 " + String.format("%.0f", avgMs) + " ms，其中慢请求（>1000ms）"
                + slowCount + " 次。");

        Map.Entry<String, Long> topType = typeStats.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        if (topType != null) {
            findings.add("操作最频繁的类型为「" + topType.getKey() + "」，共 " + topType.getValue() + " 次。");
        }

        for (String finding : findings) {
            XWPFParagraph p = doc.createParagraph();
            p.setStyle("ListBullet");
            XWPFRun r = p.createRun();
            r.setText(finding);
            r.setFontSize(10);
            r.setFontFamily("微软雅黑");
        }

        addSpacer(doc);

        XWPFParagraph note = doc.createParagraph();
        XWPFRun nr = note.createRun();
        nr.setText("注：本报告由 AI 自动生成，数据基于系统操作日志的统计分析，仅供运维参考。");
        nr.setItalic(true);
        nr.setFontSize(9);
        nr.setFontFamily("微软雅黑");
        nr.setColor("999999");
    }

    private Map<String, Long> buildCrudStats(List<OperationLogEntity> logs) {
        Map<String, Long> raw = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getRequestMethod() != null ? l.getRequestMethod().toUpperCase() : "未知",
                        Collectors.counting()));
        Map<String, Long> result = new LinkedHashMap<>();
        for (String m : CRUD_METHODS) {
            result.put(m, raw.getOrDefault(m, 0L));
        }
        raw.forEach((k, v) -> {
            if (!CRUD_METHODS.contains(k)) {
                result.put(k, v);
            }
        });
        result.values().removeIf(v -> v == 0L);
        return result;
    }

    private Map<String, Long> buildOperationTypeStats(List<OperationLogEntity> logs) {
        return logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getOperationType() != null ? l.getOperationType() : "未知",
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private Map<String, Long> buildDailyTrend(List<OperationLogEntity> logs) {
        return logs.stream()
                .filter(l -> l.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getCreateTime().toLocalDate().format(DATE_FMT),
                        TreeMap::new,
                        Collectors.counting()));
    }

    private void buildSectionTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(14);
        r.setFontFamily("微软雅黑");
        r.setColor("2E75B6");
    }

    private void buildSectionSubtitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
        r.setFontFamily("微软雅黑");
        r.setColor("404040");
    }

    private void addSimpleText(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(10);
        r.setFontFamily("微软雅黑");
    }

    private void addSpacer(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText("");
    }

    /**
     * 构建 信息键值对 表格（2列）
     */
    private void buildInfoTable(XWPFDocument doc, Map<String, String> info) {
        XWPFTable table = doc.createTable(info.size(), 2);
        setTableWidth(table, 9000);
        int row = 0;
        for (Map.Entry<String, String> e : info.entrySet()) {
            XWPFTableRow tr = table.getRow(row);
            setHeaderCell(tr, 0, e.getKey());
            setDataCell(tr, 1, e.getValue());
            row++;
        }
    }

    /**
     * 构建 统计数据 表格（3列：名称、次数、占比）
     */
    private void buildStatsTable(XWPFDocument doc, String[] headers,
                                 Map<String, Long> data, int total) {
        XWPFTable table = doc.createTable(data.size() + 1, headers.length);
        setTableWidth(table, 9000);
        for (int i = 0; i < headers.length; i++) {
            setHeaderCell(table.getRow(0), i, headers[i]);
        }
        int row = 1;
        for (Map.Entry<String, Long> e : data.entrySet()) {
            XWPFTableRow tr = table.getRow(row);
            setDataCell(tr, 0, e.getKey());
            setDataCell(tr, 1, String.valueOf(e.getValue()));
            String pct = total > 0 ? String.format("%.1f%%", e.getValue() * 100.0 / total) : "0%";
            setDataCell(tr, 2, pct);
            row++;
        }
        // 合计行
        if (headers.length == 3) {
            XWPFTableRow totalRow = table.createRow();
            setHeaderCell(totalRow, 0, "合计");
            setHeaderCell(totalRow, 1, String.valueOf(data.values().stream().mapToLong(Long::longValue).sum()));
            setHeaderCell(totalRow, 2, "100%");
        }
    }

    /**
     * 构建 DELETE 操作明细表
     */
    private void buildDeleteDetailTable(XWPFDocument doc, List<OperationLogEntity> deleteLogs) {
        List<OperationLogEntity> recent = deleteLogs.stream()
                .sorted(Comparator.comparing(OperationLogEntity::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();

        String[] headers = {"序号", "用户", "请求URL", "状态码", "耗时(ms)", "操作描述", "操作时间"};
        XWPFTable table = doc.createTable(recent.size() + 1, headers.length);
        setTableWidth(table, 9000);
        for (int i = 0; i < headers.length; i++) {
            setHeaderCell(table.getRow(0), i, headers[i]);
        }
        int row = 1;
        for (OperationLogEntity l : recent) {
            XWPFTableRow tr = table.getRow(row);
            setDataCell(tr, 0, String.valueOf(row));
            setDataCell(tr, 1, l.getUsername() != null ? l.getUsername() : "-");
            setDataCell(tr, 2, truncate(l.getRequestUrl(), 50));
            setDataCell(tr, 3, l.getResponseStatus() != null ? String.valueOf(l.getResponseStatus()) : "-");
            setDataCell(tr, 4, l.getExecutionTimeMs() != null ? String.valueOf(l.getExecutionTimeMs()) : "-");
            setDataCell(tr, 5, truncate(l.getOperationDesc(), 30));
            setDataCell(tr, 6, l.getCreateTime() != null ? l.getCreateTime().format(FMT) : "-");
            row++;
        }
    }

    private void embedImage(XWPFDocument doc, byte[] imageBytes) {
        try {
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.addPicture(new java.io.ByteArrayInputStream(imageBytes),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "chart.png",
                    Units.toEMU(460), Units.toEMU(300));
        } catch (Exception e) {
            log.warn("嵌入图表失败: {}", e.getMessage());
        }
    }

    private void setHeaderCell(XWPFTableRow row, int col, String text) {
        XWPFTableCell cell = row.getCell(col);
        cell.setColor("2E75B6");
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(10);
        r.setFontFamily("微软雅黑");
        r.setColor("FFFFFF");
    }

    private void setDataCell(XWPFTableRow row, int col, String text) {
        XWPFTableCell cell = row.getCell(col);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(col == 0 ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "");
        r.setFontSize(10);
        r.setFontFamily("微软雅黑");
    }

    private void setTableWidth(XWPFTable table, int widthTwips) {
        CTTblWidth width = table.getCTTbl().addNewTblPr().addNewTblW();
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(widthTwips));
    }

    private <V extends Comparable<V>> Map<String, V> sortByValueDesc(Map<String, V> map) {
        List<Map.Entry<String, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.<String, V>comparingByValue().reversed());
        Map<String, V> result = new LinkedHashMap<>();
        for (Map.Entry<String, V> e : list) {
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }

    private <V> Map<String, V> topN(Map<String, V> map, int n) {
        Map<String, V> result = new LinkedHashMap<>();
        int i = 0;
        for (Map.Entry<String, V> e : map.entrySet()) {
            if (i >= n) break;
            result.put(e.getKey(), e.getValue());
            i++;
        }
        return result;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    // ==================== 结构化分析方法 ====================

    @Override
    public LogAnalysisResult analyze(List<OperationLogEntity> logs, String timeRange) {
        if (logs == null || logs.isEmpty()) {
            return LogAnalysisResult.builder()
                    .totalLogs(0)
                    .timeRange(timeRange)
                    .insights(List.of(LogInsight.builder()
                            .level(Level.INFO)
                            .type(Type.GENERAL)
                            .title("暂无数据")
                            .description("统计范围内没有操作日志记录。")
                            .suggestion("请确认时间范围是否正确，或稍后再试。")
                            .build()))
                    .build();
        }

        int total = logs.size();
        Map<String, Long> crudStats = buildCrudStats(logs);
        Map<String, Long> typeStats = buildOperationTypeStats(logs);

        // DELETE 分析
        List<OperationLogEntity> deleteLogs = logs.stream()
                .filter(l -> "DELETE".equalsIgnoreCase(l.getRequestMethod())).toList();
        long deleteTotal = deleteLogs.size();
        long deleteSuccess = deleteLogs.stream()
                .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                .count();
        long deleteFailure = deleteTotal - deleteSuccess;
        double deleteSuccessRate = deleteTotal > 0 ? deleteSuccess * 100.0 / deleteTotal : 100;

        // DELETE 用户分布
        Map<String, Long> deleteUserStats = deleteLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getUsername() != null ? l.getUsername() : "未知用户",
                        Collectors.counting()));
        deleteUserStats = sortByValueDesc(deleteUserStats);

        // 高频接口
        List<TopOperation> topOps = buildTopOperations(logs, 5);

        // 错误统计
        long errorCount = logs.stream()
                .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 400).count();
        double errorRate = errorCount * 100.0 / total;

        // 平均耗时
        double avgMs = logs.stream().map(OperationLogEntity::getExecutionTimeMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).average().orElse(0);

        // 最活跃用户
        Map.Entry<String, Long> topUser = logs.stream()
                .filter(l -> l.getUsername() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getUsername, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);

        // 唯一用户数
        long uniqueUsers = logs.stream().map(OperationLogEntity::getUsername)
                .filter(Objects::nonNull).distinct().count();

        // 生成智能洞察
        List<LogInsight> insights = generateInsights(logs, crudStats, deleteLogs, deleteTotal,
                deleteSuccessRate, deleteUserStats, topOps, errorCount, errorRate, avgMs, total, topUser);

        return LogAnalysisResult.builder()
                .totalLogs(total)
                .uniqueUserCount((int) uniqueUsers)
                .timeRange(timeRange)
                .crudStats(crudStats)
                .operationTypeStats(typeStats)
                .deleteTotal(deleteTotal)
                .deleteSuccess(deleteSuccess)
                .deleteFailure(deleteFailure)
                .deleteSuccessRate(Math.round(deleteSuccessRate * 10) / 10.0)
                .topDeleteUsers(topN(deleteUserStats, 5))
                .topOperations(topOps)
                .errorCount(errorCount)
                .errorRate(Math.round(errorRate * 10) / 10.0)
                .avgTimeMs(Math.round(avgMs))
                .mostActiveUser(topUser != null ? topUser.getKey() : null)
                .mostActiveUserCount(topUser != null ? topUser.getValue() : 0)
                .insights(insights)
                .build();
    }

    /**
     * 生成智能洞察列表 — 核心智能分析逻辑
     */
    private List<LogInsight> generateInsights(List<OperationLogEntity> logs,
                                              Map<String, Long> crudStats,
                                              List<OperationLogEntity> deleteLogs,
                                              long deleteTotal,
                                              double deleteSuccessRate,
                                              Map<String, Long> deleteUserStats,
                                              List<TopOperation> topOps,
                                              long errorCount, double errorRate,
                                              double avgMs, int total,
                                              Map.Entry<String, Long> topUser) {
        List<LogInsight> insights = new ArrayList<>();

        // 1. 频繁删除操作检测
        long deleteCount = crudStats.getOrDefault("DELETE", 0L);
        double deleteRatio = total > 0 ? deleteCount * 100.0 / total : 0;
        if (deleteRatio >= 20) {
            insights.add(LogInsight.builder()
                    .level(Level.DANGER)
                    .type(Type.FREQUENT_DELETE)
                    .title("DELETE 操作占比异常偏高")
                    .description(String.format("DELETE 操作共 %d 次，占总操作量的 %.1f%%，属于高频删除行为。", deleteCount, deleteRatio))
                    .suggestion("建议立即审查删除操作的具体对象，确认是否存在误删风险。可检查 DELETE 操作的请求参数和操作描述，确保删除行为合法合规。")
                    .build());
        } else if (deleteRatio >= 5) {
            insights.add(LogInsight.builder()
                    .level(Level.WARNING)
                    .type(Type.FREQUENT_DELETE)
                    .title("DELETE 操作处于偏高水平")
                    .description(String.format("DELETE 操作共 %d 次，占比 %.1f%%，删除行为较活跃。", deleteCount, deleteRatio))
                    .suggestion("建议关注删除操作的合法性和操作者权限，定期审查删除日志。")
                    .build());
        } else if (deleteCount > 0) {
            insights.add(LogInsight.builder()
                    .level(Level.INFO)
                    .type(Type.FREQUENT_DELETE)
                    .title("DELETE 操作统计")
                    .description(String.format("DELETE 操作共 %d 次，占比 %.1f%%，整体删除行为较少。", deleteCount, deleteRatio))
                    .suggestion("删除行为处于正常水平，建议保持定期监控。")
                    .build());
        }

        // 2. DELETE 失败率检测
        if (deleteTotal > 0 && deleteSuccessRate < 85) {
            insights.add(LogInsight.builder()
                    .level(deleteSuccessRate < 70 ? Level.DANGER : Level.WARNING)
                    .type(Type.DELETE_FAILURE)
                    .title("DELETE 操作失败率" + (deleteSuccessRate < 70 ? "极高" : "较高"))
                    .description(String.format("DELETE 操作共 %d 次，其中失败 %d 次，成功率仅 %.1f%%。",
                            deleteTotal, deleteTotal - (long) (deleteSuccessRate * deleteTotal / 100), deleteSuccessRate))
                    .suggestion("建议检查：1) 删除接口的权限配置是否正确；2) 被删除对象是否存在外键约束或依赖关系；3) 接口是否存在参数校验问题。")
                    .build());
        }

        // 3. 删除操作最多的用户
        if (!deleteUserStats.isEmpty()) {
            Map.Entry<String, Long> topDeleteUser = deleteUserStats.entrySet().iterator().next();
            double userDeleteShare = deleteTotal > 0 ? topDeleteUser.getValue() * 100.0 / deleteTotal : 0;
            if (userDeleteShare >= 50 && topDeleteUser.getValue() >= 5) {
                insights.add(LogInsight.builder()
                        .level(Level.WARNING)
                        .type(Type.FREQUENT_DELETE)
                        .title("用户「" + topDeleteUser.getKey() + "」删除操作集中")
                        .description(String.format("用户「%s」执行了 %d 次 DELETE 操作，占全部删除操作的 %.1f%%。",
                                topDeleteUser.getKey(), topDeleteUser.getValue(), userDeleteShare))
                        .suggestion("建议审查该用户的删除权限是否合理，确认是否存在过度删除的风险。如有必要，可限制该用户的删除操作频率。")
                        .build());
            }
        }

        // 4. 高频接口调用检测
        for (TopOperation op : topOps) {
            if (op.getCount() >= 50) {
                String methodDesc = "DELETE".equalsIgnoreCase(op.getMethod()) ? "删除" :
                        "POST".equalsIgnoreCase(op.getMethod()) ? "新增/提交" :
                                "PUT".equalsIgnoreCase(op.getMethod()) ? "修改/更新" : "查询";
                insights.add(LogInsight.builder()
                        .level("DELETE".equalsIgnoreCase(op.getMethod()) ? Level.DANGER : Level.WARNING)
                        .type(Type.HIGH_FREQUENCY_API)
                        .title("接口「" + op.getMethod() + " " + truncate(op.getUrl(), 60) + "」被高频调用")
                        .description(String.format("该%s接口在统计期间被调用了 %d 次，属于高频操作。", methodDesc, op.getCount()))
                        .suggestion(String.format("建议关注该%s接口的调用来源和频率，确认是否存在自动化脚本调用或异常批量操作。", methodDesc))
                        .build());
            }
        }

        // 5. 高错误率检测
        if (errorRate >= 10) {
            insights.add(LogInsight.builder()
                    .level(Level.DANGER)
                    .type(Type.HIGH_ERROR_RATE)
                    .title("接口整体错误率过高")
                    .description(String.format("共发现 %d 次错误请求（状态码 >= 400），错误率 %.1f%%。", errorCount, errorRate))
                    .suggestion("建议排查系统异常原因：1) 检查错误日志中的异常堆栈；2) 确认是否为权限配置问题；3) 排查前端是否存在异常重复提交。")
                    .build());
        } else if (errorRate >= 3) {
            insights.add(LogInsight.builder()
                    .level(Level.WARNING)
                    .type(Type.HIGH_ERROR_RATE)
                    .title("接口存在一定错误率")
                    .description(String.format("共发现 %d 次错误请求，错误率 %.1f%%。", errorCount, errorRate))
                    .suggestion("建议关注失败请求的具体 URL 和原因，针对性优化。")
                    .build());
        }

        // 6. 慢请求检测
        long slowCount = logs.stream().map(OperationLogEntity::getExecutionTimeMs)
                .filter(t -> t != null && t > 1000).count();
        if (slowCount >= 10) {
            insights.add(LogInsight.builder()
                    .level(Level.WARNING)
                    .type(Type.SLOW_REQUEST)
                    .title("存在较多慢请求")
                    .description(String.format("共 %d 次请求耗时超过 1000ms，平均耗时 %.0fms。", slowCount, avgMs))
                    .suggestion("建议排查慢请求涉及的接口和数据库查询，优化 SQL 或增加缓存。")
                    .build());
        }

        // 7. 异常时段操作检测（0-6点）
        long nightOps = logs.stream()
                .filter(l -> l.getCreateTime() != null && l.getCreateTime().getHour() < 6)
                .count();
        double nightRatio = total > 0 ? nightOps * 100.0 / total : 0;
        if (nightOps >= 5 && nightRatio >= 10) {
            insights.add(LogInsight.builder()
                    .level(Level.WARNING)
                    .type(Type.ABNORMAL_TIME)
                    .title("凌晨时段存在异常操作集中")
                    .description(String.format("在 0:00-6:00 凌晨时段共有 %d 次操作（占比 %.1f%%），通常非常规工作时间。", nightOps, nightRatio))
                    .suggestion("建议核查凌晨操作的来源 IP 和用户，确认是否为定时任务、自动化脚本或潜在的安全风险。")
                    .build());
        }

        // 8. 最活跃用户提示
        if (topUser != null && topUser.getValue() >= 10) {
            insights.add(LogInsight.builder()
                    .level(Level.INFO)
                    .type(Type.ACTIVE_USER)
                    .title("最活跃用户：" + topUser.getKey())
                    .description(String.format("用户「%s」在统计期间共操作 %d 次，是系统最活跃的用户。", topUser.getKey(), topUser.getValue()))
                    .suggestion("建议关注该高频用户的操作模式，确保其操作行为符合预期。")
                    .build());
        }

        // 9. 总体概况（始终添加一条）
        insights.add(LogInsight.builder()
                .level(Level.INFO)
                .type(Type.GENERAL)
                .title("操作日志总体概况")
                .description(String.format("共分析 %d 条日志，涉及 %d 种请求方式，CRUD 分布：%s。平均耗时 %.0fms，错误率 %.1f%%。",
                        total, crudStats.size(), formatCrudSummary(crudStats), avgMs, errorRate))
                .suggestion("建议定期导出分析报告，持续监控操作趋势变化。")
                .build());

        // 按级别排序：DANGER → WARNING → INFO
        insights.sort(Comparator.comparingInt(i -> switch (i.getLevel()) {
            case DANGER -> 0;
            case WARNING -> 1;
            default -> 2;
        }));

        return insights;
    }

    private String formatCrudSummary(Map<String, Long> crudStats) {
        StringBuilder sb = new StringBuilder();
        crudStats.forEach((k, v) -> {
            if (sb.length() > 0) sb.append("、");
            sb.append(k).append(" ").append(v).append("次");
        });
        return sb.toString();
    }

    private List<TopOperation> buildTopOperations(List<OperationLogEntity> logs, int limit) {
        record UrlMethod(String url, String method) {
        }
        Map<UrlMethod, Long> freq = logs.stream()
                .filter(l -> l.getRequestUrl() != null)
                .collect(Collectors.groupingBy(
                        l -> new UrlMethod(l.getRequestUrl(),
                                l.getRequestMethod() != null ? l.getRequestMethod() : "?"),
                        Collectors.counting()));

        return freq.entrySet().stream()
                .sorted(Map.Entry.<UrlMethod, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> TopOperation.builder()
                        .method(e.getKey().method())
                        .url(e.getKey().url())
                        .count(e.getValue())
                        .build())
                .toList();
    }

    // ==================== 原有方法 ====================

    private byte[] generateEmptyReport(String username, String timeRange) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = title.createRun();
            tr.setText("操作日志分析报告");
            tr.setBold(true);
            tr.setFontSize(20);
            tr.setFontFamily("微软雅黑");
            tr.setColor("1F4E79");

            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setText("统计范围" + (timeRange != null ? "（" + timeRange + "）" : "")
                    + (username != null ? " 用户：" + username : "")
                    + " 内暂无操作日志数据，无法生成分析报告。");
            r.setFontSize(12);
            r.setFontFamily("微软雅黑");
            r.setColor("999999");

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成空报告失败: " + e.getMessage(), e);
        }
    }
}
