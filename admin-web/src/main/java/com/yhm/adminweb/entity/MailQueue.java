package com.yhm.adminweb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 邮件队列实体类
 * 映射 mail_queue 表
 */
@Entity
@Table(name = "mail_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email_id", nullable = false)
    private UUID emailId;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending"; // pending, sending, sent, failed

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", insertable = false, updatable = false)
    private Email email;

    /**
     * 获取状态显示文本
     */
    public String getStatusText() {
        return switch (status) {
            case "pending" -> "等待发送";
            case "sending" -> "发送中";
            case "sent" -> "已发送";
            case "failed" -> "发送失败";
            default -> status;
        };
    }

    /**
     * 获取状态样式类
     */
    public String getStatusClass() {
        return switch (status) {
            case "pending" -> "status-pending";
            case "sending" -> "status-sending";
            case "sent" -> "status-sent";
            case "failed" -> "status-failed";
            default -> "";
        };
    }
}

