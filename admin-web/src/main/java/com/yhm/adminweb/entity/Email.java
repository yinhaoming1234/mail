package com.yhm.adminweb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 邮件实体类
 * 映射 emails 表
 */
@Entity
@Table(name = "emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String sender;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recipients", nullable = false, columnDefinition = "VARCHAR(320)[]")
    private String[] recipients;

    @Column(length = 998)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(nullable = false)
    @Builder.Default
    private Long size = 0L;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false, length = 320)
    private String owner;

    /**
     * 格式化大小显示
     */
    public String getFormattedSize() {
        if (size == null || size == 0) return "0 B";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }

    /**
     * 获取收件人列表字符串
     */
    public String getRecipientsString() {
        if (recipients == null || recipients.length == 0) return "";
        return String.join(", ", recipients);
    }

    /**
     * 截取主题摘要
     */
    public String getSubjectSummary(int maxLength) {
        if (subject == null) return "(无主题)";
        if (subject.length() <= maxLength) return subject;
        return subject.substring(0, maxLength) + "...";
    }
}

