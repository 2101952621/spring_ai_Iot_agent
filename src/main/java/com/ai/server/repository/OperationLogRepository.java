package com.ai.server.repository;

import com.ai.server.model.entity.OperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 系统操作日志 Repository
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long>,
        JpaSpecificationExecutor<OperationLogEntity> {

    /**
     * 按用户ID查询操作日志
     */
    List<OperationLogEntity> findByUserIdOrderByCreateTimeDesc(UUID userId);

    /**
     * 按时间范围查询操作日志
     */
    @Query("SELECT o FROM OperationLogEntity o WHERE o.createTime BETWEEN :start AND :end ORDER BY o.createTime DESC")
    List<OperationLogEntity> findByCreateTimeBetween(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    /**
     * 按用户ID和时间范围查询
     */
    @Query("SELECT o FROM OperationLogEntity o WHERE o.userId = :userId AND o.createTime BETWEEN :start AND :end ORDER BY o.createTime DESC")
    List<OperationLogEntity> findByUserIdAndCreateTimeBetween(@Param("userId") UUID userId,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    /**
     * 删除指定时间范围内的日志
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OperationLogEntity o WHERE o.createTime BETWEEN :start AND :end")
    int deleteByCreateTimeBetween(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * 按操作类型查询
     */
    List<OperationLogEntity> findByOperationTypeOrderByCreateTimeDesc(String operationType);
}
