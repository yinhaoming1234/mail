package com.yhm.smtp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * SMTP 服务器配置
 */
public class SmtpServerConfig {
    
    private final String domain;
    private final int port;
    private final int maxConnections;
    private final int connectionTimeout;
    private final int readTimeout;
    private final long maxMessageSize;
    private final int maxRecipients;
    private final boolean requireAuth;
    
    private SmtpServerConfig(Builder builder) {
        this.domain = builder.domain;
        this.port = builder.port;
        this.maxConnections = builder.maxConnections;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxMessageSize = builder.maxMessageSize;
        this.maxRecipients = builder.maxRecipients;
        this.requireAuth = builder.requireAuth;
    }
    
    // ==================== Getters ====================
    
    public String getDomain() {
        return domain;
    }
    
    public int getPort() {
        return port;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public long getMaxMessageSize() {
        return maxMessageSize;
    }
    
    public int getMaxRecipients() {
        return maxRecipients;
    }
    
    public boolean isRequireAuth() {
        return requireAuth;
    }
    
    // ==================== 工厂方法 ====================
    
    /**
     * 从配置文件加载配置
     */
    public static SmtpServerConfig load() {
        Properties props = new Properties();
        
        try (InputStream is = SmtpServerConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // 使用默认配置
        }
        
        return new Builder()
                .domain(props.getProperty("smtp.domain", "localhost"))
                .port(Integer.parseInt(props.getProperty("smtp.port", "25")))
                .maxConnections(Integer.parseInt(props.getProperty("smtp.maxConnections", "1000")))
                .connectionTimeout(Integer.parseInt(props.getProperty("smtp.connectionTimeout", "60000")))
                .readTimeout(Integer.parseInt(props.getProperty("smtp.readTimeout", "300000")))
                .maxMessageSize(Long.parseLong(props.getProperty("smtp.maxMessageSize", "26214400")))
                .maxRecipients(Integer.parseInt(props.getProperty("smtp.maxRecipients", "100")))
                .requireAuth(Boolean.parseBoolean(props.getProperty("smtp.requireAuth", "false")))
                .build();
    }
    
    /**
     * 创建默认配置
     */
    public static SmtpServerConfig defaults() {
        return new Builder().build();
    }
    
    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // ==================== Builder ====================
    
    public static class Builder {
        private String domain = "localhost";
        private int port = 25;
        private int maxConnections = 1000;
        private int connectionTimeout = 60000;
        private int readTimeout = 300000;
        private long maxMessageSize = 25 * 1024 * 1024; // 25MB
        private int maxRecipients = 100;
        private boolean requireAuth = false;
        
        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }
        
        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }
        
        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }
        
        public Builder maxMessageSize(long maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
            return this;
        }
        
        public Builder maxRecipients(int maxRecipients) {
            this.maxRecipients = maxRecipients;
            return this;
        }
        
        public Builder requireAuth(boolean requireAuth) {
            this.requireAuth = requireAuth;
            return this;
        }
        
        public SmtpServerConfig build() {
            return new SmtpServerConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return "SmtpServerConfig{" +
                "domain='" + domain + '\'' +
                ", port=" + port +
                ", maxConnections=" + maxConnections +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", maxMessageSize=" + maxMessageSize +
                ", maxRecipients=" + maxRecipients +
                ", requireAuth=" + requireAuth +
                '}';
    }
}

