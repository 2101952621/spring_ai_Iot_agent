package com.ai.server.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "version_record")
public class VersionRecordEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createTime == null) this.createTime = now;
        if (this.updateTime == null) this.updateTime = now;
        if (this.sortOrder == null) this.sortOrder = 0;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
