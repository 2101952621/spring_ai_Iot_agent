package com.ai.server.repository;

import com.ai.server.model.entity.WebFunctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebFunctionRepository extends JpaRepository<WebFunctionEntity, Long> {

    /**
     * 查询所有启用的功能
     */
    List<WebFunctionEntity> findByIsEnabledTrueOrderBySortOrderAsc();

    /**
     * 按编码查询
     */
    WebFunctionEntity findByFunctionCode(String functionCode);

    @Query("""
            SELECT e FROM WebFunctionEntity e
            WHERE e.isEnabled = true
              AND (LOWER(e.functionName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.functionCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.module) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.suitableDevices) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY e.sortOrder ASC
            """)
    List<WebFunctionEntity> searchEnabledByKeyword(@Param("keyword") String keyword);
}