package com.yhm.pop3.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * POP3 服务器配置
 * 
 * 支持从配置文件加载或使用默认值
 */
public class Pop3ServerConfig {
    
    private final String domain;
    private final int port;
    private final int maxConnections;
    private final int connectionTimeout;
    private final int readTimeout;
    private final boolean requireAuth;
    private final int authFailedMaxAttempts;
    private final long autoLogoutTimeout;
    
    private Pop3ServerConfig(Builder builder) {
        this.domain = builder.domain;
        this.port = builder.port;
        this.maxConnections = builder.maxConnections;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.requireAuth = builder.requireAuth;
        this.authFailedMaxAttempts = builder.authFailedMaxAttempts;
        this.autoLogoutTimeout = builder.autoLogoutTimeout;
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
    
    public boolean isRequireAuth() {
        return requireAuth;
    }
    
    public int getAuthFailedMaxAttempts() {
        return authFailedMaxAttempts;
    }
    
    public long getAutoLogoutTimeout() {
        return autoLogoutTimeout;
    }
    
    // ==================== 工厂方法 ====================
    
    /**
     * 从配置文件加载配置
     */
    public static Pop3ServerConfig load() {
        Properties props = new Properties();
        
        try (InputStream is = Pop3ServerConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // 使用默认配置
        }
        
        return new Builder()
                .domain(props.getProperty("pop3.domain", "localhost"))
                .port(Integer.parseInt(props.getProperty("pop3.port", "110")))
                .maxConnections(Integer.parseInt(props.getProperty("pop3.maxConnections", "500")))
                .connectionTimeout(Integer.parseInt(props.getProperty("pop3.connectionTimeout", "60000")))
                .readTimeout(Integer.parseInt(props.getProperty("pop3.readTimeout", "600000")))
                .requireAuth(Boolean.parseBoolean(props.getProperty("pop3.requireAuth", "true")))
                .authFailedMaxAttempts(Integer.parseInt(props.getProperty("pop3.authFailedMaxAttempts", "3")))
                .autoLogoutTimeout(Long.parseLong(props.getProperty("pop3.autoLogoutTimeout", "600000")))
                .build();
    }
    
    /**
     * 创建默认配置
     */
    public static Pop3ServerConfig defaults() {
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
        private int port = 110;                          // POP3 标准端口
        private int maxConnections = 500;
        private int connectionTimeout = 60000;           // 60 秒
        private int readTimeout = 600000;                // 10 分钟 (RFC 1939 建议)
        private boolean requireAuth = true;
        private int authFailedMaxAttempts = 3;
        private long autoLogoutTimeout = 600000;         // 10 分钟自动登出
        
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
        
        public Builder requireAuth(boolean requireAuth) {
            this.requireAuth = requireAuth;
            return this;
        }
        
        public Builder authFailedMaxAttempts(int authFailedMaxAttempts) {
            this.authFailedMaxAttempts = authFailedMaxAttempts;
            return this;
        }
        
        public Builder autoLogoutTimeout(long autoLogoutTimeout) {
            this.autoLogoutTimeout = autoLogoutTimeout;
            return this;
        }
        
        public Pop3ServerConfig build() {
            return new Pop3ServerConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return "Pop3ServerConfig{" +
                "domain='" + domain + '\'' +
                ", port=" + port +
                ", maxConnections=" + maxConnections +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", requireAuth=" + requireAuth +
                ", authFailedMaxAttempts=" + authFailedMaxAttempts +
                ", autoLogoutTimeout=" + autoLogoutTimeout +
                '}';
    }
}

