package com.ai.server.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * IoT设备信息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "iot_device")
public class IotDeviceEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /** 设备创建时间戳（毫秒） */
    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    /** 设备类型：1-路由器，2-中继器，3-交换机，4-IPC，5-CPE，6-AP */
    @Column(name = "type", length = 255)
    private String type;

    /** 设备名称 */
    @Column(name = "name", length = 255)
    private String name;

    /** 设备标签 */
    @Column(name = "label", length = 255)
    private String label;

    /** 数据版本号 */
    @Column(name = "version")
    private Long version;

    /** 设备IP地址 */
    @Column(name = "ip", length = 64)
    private String ip;

    /** 设备MAC地址 */
    @Column(name = "mac", length = 255)
    private String mac;

    /** 设备序列号 */
    @Column(name = "sn", length = 255)
    private String sn;

    /** 软件版本 */
    @Column(name = "software_version", length = 255)
    private String softwareVersion;

    /** 硬件版本 */
    @Column(name = "firmware_version", length = 255)
    private String firmwareVersion;

    /** 设备重启状态：2-重启中 */
    @Column(name = "reboot_status", length = 1)
    private String rebootStatus;

    /** 设备是否在线 */
    @Column(name = "is_online")
    private Boolean isOnline;
}
