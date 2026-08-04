package com.ai.server.repository;

import com.ai.server.model.entity.WebFunctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}