package com.yhm.pop3;

import com.yhm.pop3.config.Pop3ServerConfig;
import com.yhm.pop3.server.Pop3Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ScopedValue;

/**
 * POP3 服务器启动入口
 * 
 * 使用 Java 25 特性：
 * - 虚拟线程（Virtual Threads）- 高并发连接处理
 * - ScopedValue（作用域值）- 比 ThreadLocal 更轻量，适合虚拟线程
 * - 结构化并发（Structured Concurrency）- 管理并发任务
 * - Record 模式匹配 - 简化数据处理
 * - 增强的 switch 表达式
 * 
 * POP3 协议实现遵循 RFC 1939
 */
public class Pop3ServerMain {
    
    private static final Logger log = LoggerFactory.getLogger(Pop3ServerMain.class);
    
    /**
     * 使用 ScopedValue 传递服务器配置
     * Java 25 新特性：比 ThreadLocal 更轻量，适合虚拟线程
     */
    public static final ScopedValue<Pop3ServerConfig> SERVER_CONFIG = ScopedValue.newInstance();
    
    /**
     * 使用 ScopedValue 传递当前会话 ID
     */
    public static final ScopedValue<String> SESSION_ID = ScopedValue.newInstance();
    
    public static void main(String[] args) {
        printBanner();
        
        log.info("==========================================");
        log.info("       POP3 Server - Java 25 Edition     ");
        log.info("==========================================");
        log.info("Java 版本: {}", System.getProperty("java.version"));
        log.info("虚拟线程支持: 已启用");
        log.info("预览特性: 已启用");
        
        // 加载配置
        Pop3ServerConfig config = Pop3ServerConfig.load();
        
        // 使用 ScopedValue 运行服务器
        ScopedValue.where(SERVER_CONFIG, config).run(() -> {
            startServer(config);
        });
    }
    
    private static void printBanner() {
        String banner = """
            
            ╔═══════════════════════════════════════════════════════════╗
            ║                                                           ║
            ║   ██████╗  ██████╗ ██████╗ ██████╗                        ║
            ║   ██╔══██╗██╔═══██╗██╔══██╗╚════██╗                       ║
            ║   ██████╔╝██║   ██║██████╔╝ █████╔╝                       ║
            ║   ██╔═══╝ ██║   ██║██╔═══╝  ╚═══██╗                       ║
            ║   ██║     ╚██████╔╝██║     ██████╔╝                       ║
            ║   ╚═╝      ╚═════╝ ╚═╝     ╚═════╝                        ║
            ║                                                           ║
            ║   POP3 Mail Server - Powered by Java 25                   ║
            ║   Virtual Threads | Scoped Values | RFC 1939              ║
            ║                                                           ║
            ╚═══════════════════════════════════════════════════════════╝
            """;
        System.out.println(banner);
    }
    
    private static void startServer(Pop3ServerConfig config) {
        Pop3Server server = new Pop3Server(config);
        
        // 注册关闭钩子，确保优雅关闭
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            log.info("收到关闭信号，正在停止 POP3 服务器...");
            server.stop();
        }));
        
        try {
            // 启动服务器
            server.start();
        } catch (IOException e) {
            log.error("启动 POP3 服务器失败: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * 获取当前配置（从 ScopedValue 中）
     */
    public static Pop3ServerConfig getCurrentConfig() {
        return SERVER_CONFIG.orElse(Pop3ServerConfig.defaults());
    }
    
    /**
     * 获取当前会话 ID
     */
    public static String getCurrentSessionId() {
        return SESSION_ID.orElse("unknown");
    }
}

