package com.yhm.pop3.protocol;

import com.yhm.mail.core.model.Email;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * POP3 会话状态
 * 
 * 维护单个 POP3 连接的状态信息
 * 
 * POP3 协议定义三种状态：
 * 1. AUTHORIZATION - 授权状态，用户认证阶段
 * 2. TRANSACTION - 事务状态，可以执行邮件操作
 * 3. UPDATE - 更新状态，提交删除操作并关闭连接
 */
public class Pop3Session {
    
    /**
     * 会话状态枚举
     */
    public enum State {
        /**
         * 授权状态 - 等待用户认证
         */
        AUTHORIZATION,
        
        /**
         * 事务状态 - 处理邮件操作
         */
        TRANSACTION,
        
        /**
         * 更新状态 - 执行删除并关闭
         */
        UPDATE
    }
    
    private State state = State.AUTHORIZATION;
    private String remoteAddress;
    private long connectionTime;
    private long lastActivityTime;
    
    // 认证相关
    private String username;
    private boolean userCommandReceived = false;
    private int authFailedAttempts = 0;
    
    // APOP 认证用的时间戳
    private final String apopTimestamp;
    
    // 邮件数据
    private List<MaildropMessage> maildrop = new ArrayList<>();
    private int totalMessageCount = 0;
    private long totalSize = 0;
    
    /**
     * 邮件箱中的邮件记录
     */
    public record MaildropMessage(
            int messageNumber,
            Email email,
            boolean deleted
    ) {
        public MaildropMessage withDeleted(boolean deleted) {
            return new MaildropMessage(messageNumber, email, deleted);
        }
        
        public long getSize() {
            return email.getSize();
        }
        
        public String getUniqueId() {
            return email.getId().toString();
        }
    }
    
    public Pop3Session(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.connectionTime = System.currentTimeMillis();
        this.lastActivityTime = this.connectionTime;
        
        // 生成 APOP 时间戳 (格式: <process-id.timestamp@hostname>)
        this.apopTimestamp = generateApopTimestamp();
    }
    
    /**
     * 生成 APOP 认证用的时间戳
     */
    private String generateApopTimestamp() {
        long processId = ProcessHandle.current().pid();
        long timestamp = System.currentTimeMillis();
        return "<" + processId + "." + timestamp + "@localhost>";
    }
    
    // ==================== 状态管理 ====================
    
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public boolean isInAuthorizationState() {
        return state == State.AUTHORIZATION;
    }
    
    public boolean isInTransactionState() {
        return state == State.TRANSACTION;
    }
    
    public boolean isInUpdateState() {
        return state == State.UPDATE;
    }
    
    // ==================== 连接信息 ====================
    
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    public long getConnectionTime() {
        return connectionTime;
    }
    
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    public void updateLastActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    public long getIdleTime() {
        return System.currentTimeMillis() - lastActivityTime;
    }
    
    // ==================== 认证相关 ====================
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isUserCommandReceived() {
        return userCommandReceived;
    }
    
    public void setUserCommandReceived(boolean userCommandReceived) {
        this.userCommandReceived = userCommandReceived;
    }
    
    public int getAuthFailedAttempts() {
        return authFailedAttempts;
    }
    
    public void incrementAuthFailedAttempts() {
        this.authFailedAttempts++;
    }
    
    public void resetAuthFailedAttempts() {
        this.authFailedAttempts = 0;
    }
    
    public String getApopTimestamp() {
        return apopTimestamp;
    }
    
    /**
     * 计算 APOP 摘要
     * digest = MD5(timestamp + password)
     */
    public String computeApopDigest(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((apopTimestamp + password).getBytes());
            
            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    // ==================== 邮件箱操作 ====================
    
    /**
     * 加载邮件箱
     */
    public void loadMaildrop(List<Email> emails) {
        this.maildrop = new ArrayList<>();
        int index = 1;
        for (Email email : emails) {
            maildrop.add(new MaildropMessage(index++, email, false));
        }
        recalculateStats();
    }
    
    /**
     * 重新计算邮件箱统计信息
     */
    private void recalculateStats() {
        this.totalMessageCount = 0;
        this.totalSize = 0;
        
        for (MaildropMessage msg : maildrop) {
            if (!msg.deleted()) {
                totalMessageCount++;
                totalSize += msg.getSize();
            }
        }
    }
    
    /**
     * 获取邮件总数（不包括已删除的）
     */
    public int getMessageCount() {
        return totalMessageCount;
    }
    
    /**
     * 获取邮件总大小（不包括已删除的）
     */
    public long getTotalSize() {
        return totalSize;
    }
    
    /**
     * 获取所有未删除的邮件
     */
    public List<MaildropMessage> getActiveMessages() {
        return maildrop.stream()
                .filter(msg -> !msg.deleted())
                .toList();
    }
    
    /**
     * 获取指定编号的邮件
     */
    public Optional<MaildropMessage> getMessage(int messageNumber) {
        if (messageNumber < 1 || messageNumber > maildrop.size()) {
            return Optional.empty();
        }
        return Optional.of(maildrop.get(messageNumber - 1));
    }
    
    /**
     * 标记邮件为已删除
     */
    public boolean markDeleted(int messageNumber) {
        if (messageNumber < 1 || messageNumber > maildrop.size()) {
            return false;
        }
        
        MaildropMessage msg = maildrop.get(messageNumber - 1);
        if (msg.deleted()) {
            return false;
        }
        
        maildrop.set(messageNumber - 1, msg.withDeleted(true));
        recalculateStats();
        return true;
    }
    
    /**
     * 重置所有删除标记
     */
    public int resetDeletedFlags() {
        int restored = 0;
        for (int i = 0; i < maildrop.size(); i++) {
            MaildropMessage msg = maildrop.get(i);
            if (msg.deleted()) {
                maildrop.set(i, msg.withDeleted(false));
                restored++;
            }
        }
        recalculateStats();
        return restored;
    }
    
    /**
     * 获取所有标记为删除的邮件
     */
    public List<MaildropMessage> getDeletedMessages() {
        return maildrop.stream()
                .filter(MaildropMessage::deleted)
                .toList();
    }
    
    /**
     * 获取删除的邮件数量
     */
    public int getDeletedCount() {
        return (int) maildrop.stream()
                .filter(MaildropMessage::deleted)
                .count();
    }
    
    @Override
    public String toString() {
        return "Pop3Session{" +
                "state=" + state +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", username='" + username + '\'' +
                ", messageCount=" + totalMessageCount +
                ", totalSize=" + totalSize +
                ", deletedCount=" + getDeletedCount() +
                '}';
    }
}

