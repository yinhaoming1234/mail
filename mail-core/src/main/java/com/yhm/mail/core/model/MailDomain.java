package com.yhm.mail.core.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 邮件域名实体类
 */
public class MailDomain {
    
    private UUID id;
    private String domain;
    private boolean local;
    private boolean enabled;
    private Instant createdAt;
    
    public MailDomain() {}
    
    private MailDomain(Builder builder) {
        this.id = builder.id;
        this.domain = builder.domain;
        this.local = builder.local;
        this.enabled = builder.enabled;
        this.createdAt = builder.createdAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getDomain() { return domain; }
    public boolean isLocal() { return local; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    
    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setLocal(boolean local) { this.local = local; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "MailDomain{" +
                "id=" + id +
                ", domain='" + domain + '\'' +
                ", local=" + local +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
    
    public static class Builder {
        private UUID id;
        private String domain;
        private boolean local;
        private boolean enabled;
        private Instant createdAt;
        
        public Builder id(UUID id) { this.id = id; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder local(boolean local) { this.local = local; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        
        public MailDomain build() {
            return new MailDomain(this);
        }
    }
}
