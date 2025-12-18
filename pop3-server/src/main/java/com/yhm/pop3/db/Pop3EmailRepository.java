package com.yhm.pop3.db;

import com.yhm.mail.core.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POP3 邮件数据访问对象
 * 负责邮件的查询和状态更新操作
 * 
 * 包含邮箱锁定功能，确保同一用户同一时间只有一个 POP3 会话
 */
public class Pop3EmailRepository {
    
    private static final Logger log = LoggerFactory.getLogger(Pop3EmailRepository.class);
    
    // 邮箱锁定记录（用户 -> 锁定时间）
    // 使用内存锁，如果需要分布式锁可以改用 Redis
    private static final Map<String, Long> maildropLocks = new ConcurrentHashMap<>();
    
    // 锁定超时时间（30分钟）
    private static final long LOCK_TIMEOUT = 30 * 60 * 1000;
    
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
     * 验证用户密码
     */
    public boolean authenticateUser(String email, String password) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE email = ? AND is_enabled = true";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password_hash");
                    // 简单比较，生产环境应使用 BCrypt 等安全哈希
                    return password.equals(storedPassword);
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取用户密码（用于 APOP 认证）
     */
    public String getUserPassword(String email) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE email = ? AND is_enabled = true";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        }
        
        return null;
    }
    
    /**
     * 查询用户的所有邮件（未删除的）
     */
    public List<Email> findByOwner(String owner) throws SQLException {
        String sql = """
            SELECT * FROM emails 
            WHERE owner = ? AND is_deleted = false 
            ORDER BY received_at DESC
            """;
        
        List<Email> emails = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, owner.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    emails.add(mapResultSetToEmail(rs));
                }
            }
        }
        
        log.debug("为用户 {} 加载了 {} 封邮件", owner, emails.size());
        return emails;
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
     * 标记邮件为已删除
     */
    public boolean markAsDeleted(UUID id) throws SQLException {
        String sql = "UPDATE emails SET is_deleted = true WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            int updated = stmt.executeUpdate();
            
            if (updated > 0) {
                log.debug("邮件 {} 已标记为删除", id);
            }
            
            return updated > 0;
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
     * 获取邮件总数
     */
    public int countByOwner(String owner) throws SQLException {
        String sql = "SELECT COUNT(*) FROM emails WHERE owner = ? AND is_deleted = false";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, owner.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * 获取邮件总大小
     */
    public long totalSizeByOwner(String owner) throws SQLException {
        String sql = "SELECT COALESCE(SUM(size), 0) FROM emails WHERE owner = ? AND is_deleted = false";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, owner.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    // ==================== 邮箱锁定 ====================
    
    /**
     * 检查邮箱是否被锁定
     */
    public boolean isMaildropLocked(String owner) {
        Long lockTime = maildropLocks.get(owner.toLowerCase());
        
        if (lockTime == null) {
            return false;
        }
        
        // 检查锁是否已过期
        if (System.currentTimeMillis() - lockTime > LOCK_TIMEOUT) {
            maildropLocks.remove(owner.toLowerCase());
            return false;
        }
        
        return true;
    }
    
    /**
     * 锁定邮箱
     */
    public void lockMaildrop(String owner) {
        maildropLocks.put(owner.toLowerCase(), System.currentTimeMillis());
        log.debug("邮箱已锁定: {}", owner);
    }
    
    /**
     * 解锁邮箱
     */
    public void unlockMaildrop(String owner) throws SQLException {
        maildropLocks.remove(owner.toLowerCase());
        log.debug("邮箱已解锁: {}", owner);
    }
    
    /**
     * 刷新锁定时间
     */
    public void refreshLock(String owner) {
        if (maildropLocks.containsKey(owner.toLowerCase())) {
            maildropLocks.put(owner.toLowerCase(), System.currentTimeMillis());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 将 ResultSet 映射为 Email 对象
     */
    private Email mapResultSetToEmail(ResultSet rs) throws SQLException {
        Array recipientsArray = rs.getArray("recipients");
        String[] recipientsArr = recipientsArray != null 
                ? (String[]) recipientsArray.getArray() 
                : new String[0];
        
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

