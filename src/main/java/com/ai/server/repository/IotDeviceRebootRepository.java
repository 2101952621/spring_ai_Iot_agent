package com.ai.server.repository;

import com.ai.server.model.entity.IotDeviceRebootEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 设备重启记录 Repository
 */
@Repository
public interface IotDeviceRebootRepository extends JpaRepository<IotDeviceRebootEntity, UUID>, JpaSpecificationExecutor<IotDeviceRebootEntity> {

    /**
     * 查询指定设备的所有重启记录，按重启时间倒序
     */
    List<IotDeviceRebootEntity> findByDeviceIdOrderByRebootTimeDesc(UUID deviceId);

    /**
     * 查询指定设备指定状态的重启记录
     */
    List<IotDeviceRebootEntity> findByDeviceIdAndRebootStatusOrderByRebootTimeDesc(UUID deviceId, String rebootStatus);
}
