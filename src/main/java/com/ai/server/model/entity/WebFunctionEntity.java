package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 网页功能配置表
 * 用于描述"AI一键打开网页功能"的业务数据项等。
 */
@Data
@Entity
@Table(name = "web_function")
public class WebFunctionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "function_code", nullable = false, unique = true, length = 100)
    private String functionCode;

    @Column(name = "function_name", nullable = false, length = 200)
    private String functionName;

    @Column(name = "module", length = 100)
    private String module;

    @Column(name = "function_path", length = 500)
    private String functionPath;

    @Column(name = "open_url", length = 500)
    private String openUrl;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "config_method", columnDefinition = "TEXT")
    private String configMethod;

    @Column(name = "config_steps", columnDefinition = "TEXT")
    private String configSteps;

    @Column(name = "precautions", columnDefinition = "TEXT")
    private String precautions;

    @Column(name = "suitable_devices", columnDefinition = "TEXT")
    private String suitableDevices;

    @Column(name = "icon", length = 200)
    private String icon;

    @Column(name = "button_text", length = 100)
    private String buttonText;

    @Column(name = "card_type", length = 50)
    private String cardType;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @PrePersist
    protected void prePersist() {
        Date now = new Date();
        if (this.createTime == null) this.createTime = now;
        if (this.updateTime == null) this.updateTime = now;
        if (this.buttonText == null || this.buttonText.isBlank()) this.buttonText = "打开";
        if (this.cardType == null || this.cardType.isBlank()) this.cardType = "WEB_FUNCTION";
        if (this.isEnabled == null) this.isEnabled = true;
        if (this.sortOrder == null) this.sortOrder = 0;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updateTime = new Date();
    }
}