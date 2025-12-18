package com.yhm.mail.core.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户实体类
 */
public class User {
    
    private UUID id;
    private String username;
    private String domain;
    private String email;
    private String passwordHash;
    private long quotaBytes;
    private long usedBytes;
    private boolean enabled;
    private Instant createdAt;
    private Instant lastLoginAt;
    
    public User() {}
    
    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.domain = builder.domain;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.quotaBytes = builder.quotaBytes;
        this.usedBytes = builder.usedBytes;
        this.enabled = builder.enabled;
        this.createdAt = builder.createdAt;
        this.lastLoginAt = builder.lastLoginAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getDomain() { return domain; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public long getQuotaBytes() { return quotaBytes; }
    public long getUsedBytes() { return usedBytes; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    
    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setQuotaBytes(long quotaBytes) { this.quotaBytes = quotaBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", domain='" + domain + '\'' +
                ", email='" + email + '\'' +
                ", quotaBytes=" + quotaBytes +
                ", usedBytes=" + usedBytes +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
    
    public static class Builder {
        private UUID id;
        private String username;
        private String domain;
        private String email;
        private String passwordHash;
        private long quotaBytes;
        private long usedBytes;
        private boolean enabled;
        private Instant createdAt;
        private Instant lastLoginAt;
        
        public Builder id(UUID id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder quotaBytes(long quotaBytes) { this.quotaBytes = quotaBytes; return this; }
        public Builder usedBytes(long usedBytes) { this.usedBytes = usedBytes; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder lastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        
        public User build() {
            return new User(this);
        }
    }
}
