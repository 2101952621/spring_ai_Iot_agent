package com.ai.server.service;

import com.ai.server.model.entity.OperationLogEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 系统操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 保存操作日志
     */
    void save(OperationLogEntity log);

    /**
     * 按用户ID查询
     */
    List<OperationLogEntity> findByUserId(UUID userId);

    /**
     * 按时间范围查询
     */
    List<OperationLogEntity> findByTimeRange(LocalDateTime start, LocalDateTime end);

    /**
     * 按用户ID和时间范围查询
     */
    List<OperationLogEntity> findByUserIdAndTimeRange(UUID userId, LocalDateTime start, LocalDateTime end);

    /**
     * 删除指定时间范围内的日志
     * @return 删除条数
     */
    int deleteByTimeRange(LocalDateTime start, LocalDateTime end);

    /**
     * 按操作类型查询
     */
    List<OperationLogEntity> findByOperationType(String operationType);

    /**
     * 导出用户日志为Excel字节数组
     * @param userId 用户ID，为null则导出全部
     * @param start 开始时间，为null则不限制
     * @param end 结束时间，为null则不限制
     * @return Excel文件字节数组
     */
    byte[] exportToExcel(UUID userId, LocalDateTime start, LocalDateTime end);
}
