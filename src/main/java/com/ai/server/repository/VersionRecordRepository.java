package com.ai.server.repository;

import com.ai.server.model.entity.VersionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VersionRecordRepository extends JpaRepository<VersionRecordEntity, Long> {

    List<VersionRecordEntity> findAllByOrderBySortOrderDesc();
}
