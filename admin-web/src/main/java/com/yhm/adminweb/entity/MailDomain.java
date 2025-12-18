package com.yhm.adminweb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 邮件域名实体类
 * 映射 mail_domains 表
 */
@Entity
@Table(name = "mail_domains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_local", nullable = false)
    @Builder.Default
    private Boolean isLocal = true;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

