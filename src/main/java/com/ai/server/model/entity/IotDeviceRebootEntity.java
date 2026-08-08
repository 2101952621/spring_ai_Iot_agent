package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 设备重启操作记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "iot_device_reboot")
public class IotDeviceRebootEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "device_type", length = 255)
    private String deviceType;

    @Column(name = "device_sn", length = 255)
    private String deviceSn;

    @Column(name = "device_ip", length = 64)
    private String deviceIp;

    @Column(name = "reboot_time", nullable = false)
    private Long rebootTime;

    @Column(name = "reboot_status", length = 20, nullable = false)
    private String rebootStatus;

    @Column(name = "result_desc", length = 500)
    private String resultDesc;

    @Column(name = "operator", length = 255)
    private String operator;

    @Column(name = "remark", length = 500)
    private String remark;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
