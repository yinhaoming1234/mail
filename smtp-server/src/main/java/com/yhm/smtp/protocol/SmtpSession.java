package com.yhm.smtp.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * SMTP 会话状态
 * 维护单个 SMTP 连接的状态信息
 */
public class SmtpSession {
    
    /**
     * 会话状态枚举
     */
    public enum State {
        /**
         * 初始状态，等待 HELO/EHLO
         */
        INIT,
        
        /**
         * 已握手，等待 MAIL FROM
         */
        READY,
        
        /**
         * 已设置发件人，等待 RCPT TO
         */
        MAIL_FROM_SET,
        
        /**
         * 已设置收件人，可以发送 DATA 或添加更多收件人
         */
        RCPT_TO_SET,
        
        /**
         * 正在接收邮件数据
         */
        DATA,
        
        /**
         * 会话结束
         */
        QUIT
    }
    
    private State state = State.INIT;
    private String clientHostname;
    private String sender;
    private final List<String> recipients = new ArrayList<>();
    private final StringBuilder mailData = new StringBuilder();
    private final String remoteAddress;
    private final long connectionTime;
    private boolean extendedMode = false;
    
    public SmtpSession(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.connectionTime = System.currentTimeMillis();
    }
    
    // ==================== 状态管理 ====================
    
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public boolean isExtendedMode() {
        return extendedMode;
    }
    
    public void setExtendedMode(boolean extendedMode) {
        this.extendedMode = extendedMode;
    }
    
    // ==================== 客户端信息 ====================
    
    public String getClientHostname() {
        return clientHostname;
    }
    
    public void setClientHostname(String clientHostname) {
        this.clientHostname = clientHostname;
    }
    
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    public long getConnectionTime() {
        return connectionTime;
    }
    
    // ==================== 邮件信息 ====================
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public List<String> getRecipients() {
        return recipients;
    }
    
    public void addRecipient(String recipient) {
        this.recipients.add(recipient);
    }
    
    public void clearRecipients() {
        this.recipients.clear();
    }
    
    // ==================== 邮件数据 ====================
    
    public StringBuilder getMailData() {
        return mailData;
    }
    
    public void appendMailData(String line) {
        if (!mailData.isEmpty()) {
            mailData.append("\r\n");
        }
        mailData.append(line);
    }
    
    public String getMailDataString() {
        return mailData.toString();
    }
    
    public void clearMailData() {
        mailData.setLength(0);
    }
    
    // ==================== 重置 ====================
    
    /**
     * 重置邮件事务状态
     * 保留连接状态和客户端信息
     */
    public void resetTransaction() {
        this.sender = null;
        this.recipients.clear();
        this.mailData.setLength(0);
        if (this.state != State.INIT && this.state != State.QUIT) {
            this.state = State.READY;
        }
    }
    
    /**
     * 完全重置会话
     */
    public void reset() {
        this.state = State.INIT;
        this.clientHostname = null;
        this.sender = null;
        this.recipients.clear();
        this.mailData.setLength(0);
        this.extendedMode = false;
    }
    
    @Override
    public String toString() {
        return "SmtpSession{" +
                "state=" + state +
                ", clientHostname='" + clientHostname + '\'' +
                ", sender='" + sender + '\'' +
                ", recipients=" + recipients +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", extendedMode=" + extendedMode +
                '}';
    }
}

