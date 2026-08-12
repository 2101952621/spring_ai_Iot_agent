package com.ai.server.service;

import com.ai.server.model.entity.OperationLogEntity;
import com.ai.server.model.vo.LogAnalysisResult;

import java.util.List;

/**
 * 操作日志分析报告生成服务
 */
public interface LogAnalysisReportService {

    byte[] generateAnalysisReport(List<OperationLogEntity> logs, String username, String timeRange);

    LogAnalysisResult analyze(List<OperationLogEntity> logs, String timeRange);
}
