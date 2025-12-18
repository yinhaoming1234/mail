package com.yhm.adminweb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 用户实体类
 * 映射 users 表
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String domain;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "quota_bytes", nullable = false)
    @Builder.Default
    private Long quotaBytes = 1073741824L; // 默认 1GB

    @Column(name = "used_bytes", nullable = false)
    @Builder.Default
    private Long usedBytes = 0L;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /**
     * 获取已使用配额百分比
     */
    public double getUsedPercentage() {
        if (quotaBytes == null || quotaBytes == 0) return 0;
        return (double) usedBytes / quotaBytes * 100;
    }

    /**
     * 格式化配额显示
     */
    public String getFormattedQuota() {
        return formatBytes(quotaBytes);
    }

    /**
     * 格式化已用空间显示
     */
    public String getFormattedUsed() {
        return formatBytes(usedBytes);
    }

    private String formatBytes(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

