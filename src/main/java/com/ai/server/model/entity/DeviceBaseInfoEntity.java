package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备基础信息实体
 */
@Data
@Entity
@Table(name = "device_base_info")
public class DeviceBaseInfoEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_name", length = 200)
    private String deviceName;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "scene", columnDefinition = "TEXT")
    private String scene;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "core", columnDefinition = "TEXT")
    private String core;

    @Column(name = "product_url", length = 500)
    private String productUrl;
}
