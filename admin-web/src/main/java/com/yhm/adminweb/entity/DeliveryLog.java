package com.yhm.adminweb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 发送日志实体类
 * 映射 delivery_logs 表
 */
@Entity
@Table(name = "delivery_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email_id", nullable = false)
    private UUID emailId;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(name = "remote_host", length = 255)
    private String remoteHost;

    @Column(name = "remote_ip", length = 45)
    private String remoteIp;

    @Column(nullable = false, length = 20)
    private String status; // delivered, bounced, deferred

    @Column(name = "smtp_code")
    private Integer smtpCode;

    @Column(name = "smtp_response", columnDefinition = "TEXT")
    private String smtpResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", insertable = false, updatable = false)
    private Email email;

    /**
     * 获取状态显示文本
     */
    public String getStatusText() {
        return switch (status) {
            case "delivered" -> "已送达";
            case "bounced" -> "退回";
            case "deferred" -> "延迟";
            default -> status;
        };
    }

    /**
     * 获取状态样式类
     */
    public String getStatusClass() {
        return switch (status) {
            case "delivered" -> "status-delivered";
            case "bounced" -> "status-bounced";
            case "deferred" -> "status-deferred";
            default -> "";
        };
    }
}

