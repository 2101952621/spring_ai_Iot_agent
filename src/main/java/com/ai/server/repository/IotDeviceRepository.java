package com.ai.server.repository;

import com.ai.server.model.entity.IotDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * IoT设备信息 Repository
 */
@Repository
public interface IotDeviceRepository extends JpaRepository<IotDeviceEntity, UUID>, JpaSpecificationExecutor<IotDeviceEntity> {

    /**
     * 根据设备名称模糊查询
     */
    List<IotDeviceEntity> findByNameContaining(String name);

    /**
     * 根据设备序列号精确查询
     */
    Optional<IotDeviceEntity> findBySn(String sn);

    /**
     * 根据MAC地址查询
     */
    Optional<IotDeviceEntity> findByMac(String mac);

    /**
     * 根据IP地址查询
     */
    Optional<IotDeviceEntity> findByIp(String ip);

    /**
     * 根据设备类型查询
     */
    List<IotDeviceEntity> findByType(String type);

    /**
     * 查询所有在线设备
     */
    List<IotDeviceEntity> findByIsOnlineTrue();

    /**
     * 根据名称精确查询
     */
    Optional<IotDeviceEntity> findByName(String name);
}
