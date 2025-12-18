package com.yhm.mail.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 邮件实体类
 * 存储完整的邮件信息
 */
public class Email {
    
    private UUID id;
    private String sender;
    private List<String> recipients;
    private String subject;
    private String body;
    private String rawContent;
    private long size;
    private Instant receivedAt;
    private boolean read;
    private boolean deleted;
    private String owner;
    
    public Email() {
        this.recipients = new ArrayList<>();
    }
    
    private Email(Builder builder) {
        this.id = builder.id;
        this.sender = builder.sender;
        this.recipients = builder.recipients != null ? builder.recipients : new ArrayList<>();
        this.subject = builder.subject;
        this.body = builder.body;
        this.rawContent = builder.rawContent;
        this.size = builder.size;
        this.receivedAt = builder.receivedAt;
        this.read = builder.read;
        this.deleted = builder.deleted;
        this.owner = builder.owner;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getSender() { return sender; }
    public List<String> getRecipients() { return recipients; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getRawContent() { return rawContent; }
    public long getSize() { return size; }
    public Instant getReceivedAt() { return receivedAt; }
    public boolean isRead() { return read; }
    public boolean isDeleted() { return deleted; }
    public String getOwner() { return owner; }
    
    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setSender(String sender) { this.sender = sender; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBody(String body) { this.body = body; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }
    public void setSize(long size) { this.size = size; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public void setRead(boolean read) { this.read = read; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public void setOwner(String owner) { this.owner = owner; }
    
    @Override
    public String toString() {
        return "Email{" +
                "id=" + id +
                ", sender='" + sender + '\'' +
                ", recipients=" + recipients +
                ", subject='" + subject + '\'' +
                ", size=" + size +
                ", receivedAt=" + receivedAt +
                ", read=" + read +
                ", deleted=" + deleted +
                ", owner='" + owner + '\'' +
                '}';
    }
    
    public static class Builder {
        private UUID id;
        private String sender;
        private List<String> recipients;
        private String subject;
        private String body;
        private String rawContent;
        private long size;
        private Instant receivedAt;
        private boolean read;
        private boolean deleted;
        private String owner;
        
        public Builder id(UUID id) { this.id = id; return this; }
        public Builder sender(String sender) { this.sender = sender; return this; }
        public Builder recipients(List<String> recipients) { this.recipients = recipients; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder rawContent(String rawContent) { this.rawContent = rawContent; return this; }
        public Builder size(long size) { this.size = size; return this; }
        public Builder receivedAt(Instant receivedAt) { this.receivedAt = receivedAt; return this; }
        public Builder read(boolean read) { this.read = read; return this; }
        public Builder deleted(boolean deleted) { this.deleted = deleted; return this; }
        public Builder owner(String owner) { this.owner = owner; return this; }
        
        public Email build() {
            return new Email(this);
        }
    }
}
