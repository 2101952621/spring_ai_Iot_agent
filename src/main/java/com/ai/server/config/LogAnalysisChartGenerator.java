package com.ai.server.config;

import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 操作日志统计分析图表生成器
 * 基于 JFreeChart 生成饼图、折线图、柱状图，输出 PNG 字节数组供 Word 文档嵌入
 */
@Slf4j
public class LogAnalysisChartGenerator {

    /** 图表默认宽高 */
    private static final int WIDTH = 560;
    private static final int HEIGHT = 360;

    /** 配色方案（高对比度） */
    private static final Color[] PALETTE = {
            new Color(64, 158, 255),   // 蓝
            new Color(103, 194, 58),   // 绿
            new Color(230, 162, 60),   // 橙
            new Color(245, 108, 108),  // 红
            new Color(144, 147, 153),  // 灰
            new Color(155, 89, 182),   // 紫
            new Color(52, 152, 219),   // 浅蓝
            new Color(241, 196, 15)    // 黄
    };

    private LogAnalysisChartGenerator() {
    }

    /**
     * 查找支持中文的字体，按优先级尝试
     */
    private static Font resolveCjkFont(int style, float size) {
        String[] candidates = {
                "Microsoft YaHei", "微软雅黑", "SimHei", "黑体",
                "Noto Sans CJK SC", "WenQuanYi Micro Hei", "PingFang SC",
                "Source Han Sans SC", "Arial Unicode MS", "SansSerif"
        };
        for (String name : candidates) {
            Font f = new Font(name, style, (int) size);
            if (f.canDisplayUpTo("操作日志分析报告") == -1) {
                return f;
            }
        }
        return new Font("SansSerif", style, (int) size);
    }

    static Font titleFont(float size) {
        return resolveCjkFont(Font.BOLD, size);
    }

    static Font plainFont(float size) {
        return resolveCjkFont(Font.PLAIN, size);
    }

    /**
     * 将 JFreeChart 渲染为 PNG 字节数组
     */
    private static byte[] renderToPng(JFreeChart chart, int width, int height) {
        applyChartStyle(chart);
        BufferedImage image = chart.createBufferedImage(width, height);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("图表渲染失败", e);
            throw new RuntimeException("图表渲染失败: " + e.getMessage(), e);
        }
    }

    private static void applyChartStyle(JFreeChart chart) {
        Font titleF = titleFont(16f);
        Font subF = plainFont(11f);
        Font tickF = plainFont(11f);
        Font legendF = plainFont(12f);

        if (chart.getTitle() != null) {
            chart.getTitle().setFont(titleF);
        }
        chart.setBackgroundPaint(Color.WHITE);
        chart.setPadding(new RectangleInsets(10, 10, 10, 10));

        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(legendF);
        }

        if (chart.getPlot() instanceof CategoryPlot plot) {
            plot.setBackgroundPaint(new Color(250, 250, 250));
            plot.setRangeGridlinePaint(new Color(220, 220, 220));
            plot.setOutlinePaint(new Color(200, 200, 200));
            if (plot.getDomainAxis() != null) {
                plot.getDomainAxis().setTickLabelFont(tickF);
                plot.getDomainAxis().setLabelFont(subF);
            }
            if (plot.getRangeAxis() != null) {
                plot.getRangeAxis().setTickLabelFont(tickF);
                plot.getRangeAxis().setLabelFont(subF);
            }
            if (plot.getRenderer() instanceof BarRenderer br) {
                br.setItemMargin(0.05);
                br.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
            }
            if (plot.getRenderer() instanceof LineAndShapeRenderer lr) {
                lr.setDefaultShapesVisible(true);
            }
        } else if (chart.getPlot() instanceof PiePlot<?> plot) {
            plot.setLabelFont(tickF);
            plot.setDefaultSectionOutlinePaint(Color.WHITE);
            plot.setDefaultSectionOutlineStroke(new BasicStroke(1.5f));
        }
    }

    private static void applyPieColors(PiePlot<?> plot, int count) {
        for (int i = 0; i < count; i++) {
            plot.setSectionPaint(i, PALETTE[i % PALETTE.length]);
        }
    }

    /**
     * 生成饼图
     *
     * @param title     图表标题
     * @param data      数据（label → value）
     * @return PNG 字节数组
     */
    public static byte[] createPieChart(String title, Map<String, ? extends Number> data) {
        if (data == null || data.isEmpty()) {
            return createEmptyChart(title);
        }
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        data.forEach((k, v) -> dataset.setValue(k == null || k.isBlank() ? "未知" : k, v));

        JFreeChart chart = ChartFactory.createPieChart(
                title, dataset, true, true, false);
        PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
        applyPieColors(plot, dataset.getItemCount());
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} {2}"));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        plot.setShadowPaint(null);
        plot.setCircular(true);

        return renderToPng(chart, WIDTH, HEIGHT);
    }

    /**
     * 生成折线趋势图
     *
     * @param title    图表标题
     * @param xLabel   X轴标签
     * @param yLabel   Y轴标签
     * @param series   数据系列（seriesName → (xLabel → value)）
     * @return PNG 字节数组
     */
    public static byte[] createLineChart(String title, String xLabel, String yLabel,
                                         Map<String, Map<String, ? extends Number>> series) {
        if (series == null || series.isEmpty()) {
            return createEmptyChart(title);
        }
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        series.forEach((seriesName, points) -> {
            if (points != null) {
                points.forEach((x, y) -> dataset.addValue(y, seriesName, x));
            }
        });

        JFreeChart chart = ChartFactory.createLineChart(
                title, xLabel, yLabel, dataset,
                PlotOrientation.VERTICAL, true, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        int idx = 0;
        for (String ignored : series.keySet()) {
            renderer.setSeriesPaint(idx, PALETTE[idx % PALETTE.length]);
            renderer.setSeriesStroke(idx, new BasicStroke(2.5f));
            idx++;
        }

        return renderToPng(chart, WIDTH, HEIGHT);
    }

    /**
     * 生成单系列折线趋势图
     */
    public static byte[] createLineChart(String title, String xLabel, String yLabel,
                                         String seriesName, Map<String, ? extends Number> points) {
        return createLineChart(title, xLabel, yLabel, Map.of(seriesName, points));
    }

    /**
     * 生成柱状图
     *
     * @param title    图表标题
     * @param xLabel   X轴标签
     * @param yLabel   Y轴标签
     * @param seriesName 系列名称
     * @param data     数据（category → value），按 value 降序排列的前 N 项
     * @return PNG 字节数组
     */
    public static byte[] createBarChart(String title, String xLabel, String yLabel,
                                        String seriesName, Map<String, ? extends Number> data) {
        if (data == null || data.isEmpty()) {
            return createEmptyChart(title);
        }
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((k, v) -> dataset.addValue(v, seriesName, k == null || k.isBlank() ? "未知" : k));

        JFreeChart chart = ChartFactory.createBarChart(
                title, xLabel, yLabel, dataset,
                PlotOrientation.VERTICAL, false, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, PALETTE[0]);

        return renderToPng(chart, WIDTH, HEIGHT);
    }

    // ==================== 空图表占位 ====================

    private static byte[] createEmptyChart(String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                title + "（暂无数据）", "", "", dataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.addSubtitle(new TextTitle("暂无数据", plainFont(12f)));
        return renderToPng(chart, WIDTH, HEIGHT);
    }

    /**
     * 从有序键值对构建 LinkedHashMap（保持顺序）
     */
    public static <T> java.util.LinkedHashMap<String, T> ordered(List<String> keys, java.util.function.Function<String, T> valFn) {
        java.util.LinkedHashMap<String, T> map = new java.util.LinkedHashMap<>();
        for (String k : keys) {
            map.put(k, valFn.apply(k));
        }
        return map;
    }
}
