package com.yhm.smtp.db;

import com.yhm.mail.core.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 邮件数据访问对象
 * 负责邮件的增删改查操作
 */
public class EmailRepository {
    
    private static final Logger log = LoggerFactory.getLogger(EmailRepository.class);
    
    /**
     * 保存邮件
     *
     * @param email 邮件对象
     * @return 保存后的邮件（包含生成的ID）
     */
    public Email save(Email email) throws SQLException {
        String sql = """
            INSERT INTO emails (id, sender, recipients, subject, body, raw_content, size, received_at, is_read, is_deleted, owner)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                is_read = EXCLUDED.is_read,
                is_deleted = EXCLUDED.is_deleted
            RETURNING id
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            UUID id = email.getId() != null ? email.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setString(2, email.getSender());
            stmt.setArray(3, conn.createArrayOf("varchar", email.getRecipients().toArray()));
            stmt.setString(4, email.getSubject());
            stmt.setString(5, email.getBody());
            stmt.setString(6, email.getRawContent());
            stmt.setLong(7, email.getSize());
            stmt.setTimestamp(8, Timestamp.from(email.getReceivedAt() != null ? email.getReceivedAt() : Instant.now()));
            stmt.setBoolean(9, email.isRead());
            stmt.setBoolean(10, email.isDeleted());
            stmt.setString(11, email.getOwner());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    email.setId(rs.getObject(1, UUID.class));
                }
            }
            
            log.debug("邮件已保存: id={}, from={}, to={}", id, email.getSender(), email.getRecipients());
            return email;
        }
    }
    
    /**
     * 批量保存邮件（为每个收件人创建一份）
     *
     * @param email 原始邮件
     * @return 保存的邮件列表
     */
    public List<Email> saveForRecipients(Email email) throws SQLException {
        List<Email> savedEmails = new ArrayList<>();
        
        for (String recipient : email.getRecipients()) {
            Email copy = Email.builder()
                    .id(UUID.randomUUID())
                    .sender(email.getSender())
                    .recipients(List.of(recipient))
                    .subject(email.getSubject())
                    .body(email.getBody())
                    .rawContent(email.getRawContent())
                    .size(email.getSize())
                    .receivedAt(email.getReceivedAt())
                    .read(false)
                    .deleted(false)
                    .owner(recipient)
                    .build();
            
            savedEmails.add(save(copy));
        }
        
        return savedEmails;
    }
    
    /**
     * 根据ID查询邮件
     */
    public Optional<Email> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM emails WHERE id = ? AND is_deleted = false";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmail(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 查询用户的所有邮件
     */
    public List<Email> findByOwner(String owner) throws SQLException {
        String sql = "SELECT * FROM emails WHERE owner = ? AND is_deleted = false ORDER BY received_at DESC";
        
        List<Email> emails = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, owner);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    emails.add(mapResultSetToEmail(rs));
                }
            }
        }
        
        return emails;
    }
    
    /**
     * 查询用户未读邮件数量
     */
    public int countUnreadByOwner(String owner) throws SQLException {
        String sql = "SELECT COUNT(*) FROM emails WHERE owner = ? AND is_read = false AND is_deleted = false";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, owner);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * 标记邮件为已读
     */
    public boolean markAsRead(UUID id) throws SQLException {
        String sql = "UPDATE emails SET is_read = true WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 标记邮件为已删除
     */
    public boolean markAsDeleted(UUID id) throws SQLException {
        String sql = "UPDATE emails SET is_deleted = true WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 物理删除邮件
     */
    public boolean delete(UUID id) throws SQLException {
        String sql = "DELETE FROM emails WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 检查域名是否为本地域名
     */
    public boolean isLocalDomain(String domain) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mail_domains WHERE domain = ? AND is_local = true AND is_enabled = true";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, domain.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查用户是否存在
     */
    public boolean userExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND is_enabled = true";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 将 ResultSet 映射为 Email 对象
     */
    private Email mapResultSetToEmail(ResultSet rs) throws SQLException {
        Array recipientsArray = rs.getArray("recipients");
        String[] recipientsArr = recipientsArray != null ? (String[]) recipientsArray.getArray() : new String[0];
        
        return Email.builder()
                .id(rs.getObject("id", UUID.class))
                .sender(rs.getString("sender"))
                .recipients(Arrays.asList(recipientsArr))
                .subject(rs.getString("subject"))
                .body(rs.getString("body"))
                .rawContent(rs.getString("raw_content"))
                .size(rs.getLong("size"))
                .receivedAt(rs.getTimestamp("received_at").toInstant())
                .read(rs.getBoolean("is_read"))
                .deleted(rs.getBoolean("is_deleted"))
                .owner(rs.getString("owner"))
                .build();
    }
}

