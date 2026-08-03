package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备基础信息实体
 * 映射 public.device_base_info 表
 */
@Data
@Entity
@Table(name = "device_base_info")
public class DeviceBaseInfoEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 设备名称 */
    @Column(name = "device_name", length = 200, nullable = false)
    private String deviceName;

    /** 设备型号 */
    @Column(name = "device_model", length = 100, nullable = false)
    private String deviceModel;

    /** 设备类型：ROUTER / SWITCH / REPEATER */
    @Column(name = "device_type", length = 20, nullable = false)
    private String deviceType;

    /** 产品分类 */
    @Column(name = "category", length = 100)
    private String category;

    /** 品牌（默认 MTN） */
    @Column(name = "brand", length = 50)
    private String brand;

    /** 价格 */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    /** 产品描述 */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 核心功能 */
    @Column(name = "core", columnDefinition = "TEXT")
    private String core;

    /** 规格参数（JSON） */
    @Column(name = "specs", columnDefinition = "jsonb")
    private String specs;

    /** 适用场景 */
    @Column(name = "suitable_scenarios", columnDefinition = "TEXT")
    private String suitableScenarios;

    /** 官网链接 */
    @Column(name = "product_url", length = 500)
    private String productUrl;

    /** 是否推荐 */
    @Column(name = "is_recommended", nullable = false)
    private Boolean isRecommended;

    /** 排序 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 创建时间 */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 创建人 */
    @Column(name = "creater", nullable = false)
    private Long creater;

    /** 更新人 */
    @Column(name = "updater", nullable = false)
    private Long updater;
}
