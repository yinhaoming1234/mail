package com.yhm.smtp;

import com.yhm.smtp.config.SmtpServerConfig;
import com.yhm.smtp.server.SmtpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ScopedValue;

/**
 * SMTP 服务器启动入口
 * 
 * 使用 Java 25 特性：
 * - 虚拟线程（Virtual Threads）
 * - ScopedValue（作用域值）
 * - 结构化并发（Structured Concurrency）
 */
public class SmtpServerMain {
    
    private static final Logger log = LoggerFactory.getLogger(SmtpServerMain.class);
    
    /**
     * 使用 ScopedValue 传递服务器配置
     * Java 25 新特性：比 ThreadLocal 更轻量，适合虚拟线程
     */
    public static final ScopedValue<SmtpServerConfig> SERVER_CONFIG = ScopedValue.newInstance();
    
    public static void main(String[] args) {
        log.info("==========================================");
        log.info("       SMTP Server - Java 25 Edition     ");
        log.info("==========================================");
        log.info("Java 版本: {}", System.getProperty("java.version"));
        log.info("虚拟线程支持: 已启用");
        
        // 加载配置
        SmtpServerConfig config = SmtpServerConfig.load();
        
        // 使用 ScopedValue 运行服务器
        ScopedValue.where(SERVER_CONFIG, config).run(() -> {
            startServer(config);
        });
    }
    
    private static void startServer(SmtpServerConfig config) {
        SmtpServer server = new SmtpServer(config);
        
        // 注册关闭钩子，确保优雅关闭
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            log.info("收到关闭信号，正在停止服务器...");
            server.stop();
        }));
        
        try {
            // 启动服务器
            server.start();
        } catch (IOException e) {
            log.error("启动服务器失败: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}

