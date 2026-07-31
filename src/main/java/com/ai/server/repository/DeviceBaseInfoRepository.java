package com.ai.server.repository;

import com.ai.server.model.entity.DeviceBaseInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceBaseInfoRepository extends JpaRepository<DeviceBaseInfoEntity, Long> {
}
