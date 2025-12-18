package com.yhm.smtp.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库配置管理类
 * 使用 HikariCP 连接池管理 PostgreSQL 连接
 */
public final class DatabaseConfig {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    
    private static volatile HikariDataSource dataSource;
    private static final Object LOCK = new Object();
    
    private DatabaseConfig() {
        // 工具类，禁止实例化
    }
    
    /**
     * 获取数据源
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }
    
    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
    
    /**
     * 创建 HikariCP 数据源
     */
    private static HikariDataSource createDataSource() {
        Properties props = loadProperties();
        
        HikariConfig config = new HikariConfig();
        
        // 基本连接配置
        config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://localhost:5432/maildb"));
        config.setUsername(props.getProperty("db.username", "postgres"));
        config.setPassword(props.getProperty("db.password", "postgres"));
        config.setDriverClassName("org.postgresql.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
        
        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        
        // 连接池名称
        config.setPoolName("SmtpMailPool");
        
        // PostgreSQL 特定优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        log.info("初始化数据库连接池: {}", config.getJdbcUrl());
        
        return new HikariDataSource(config);
    }
    
    /**
     * 加载配置文件
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        
        try (InputStream is = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
                log.info("已加载配置文件 application.properties");
            } else {
                log.warn("未找到配置文件 application.properties，使用默认配置");
            }
        } catch (IOException e) {
            log.warn("加载配置文件失败，使用默认配置: {}", e.getMessage());
        }
        
        return props;
    }
    
    /**
     * 关闭数据源
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("数据库连接池已关闭");
        }
    }
}

