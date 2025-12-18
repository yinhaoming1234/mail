package com.yhm.smtp.server;

import com.yhm.smtp.config.SmtpServerConfig;
import com.yhm.smtp.db.DatabaseConfig;
import com.yhm.smtp.db.DatabaseInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SMTP 服务器
 * 使用 Java 25 虚拟线程处理并发连接
 * 
 * 主要特性：
 * - 使用虚拟线程（Virtual Threads）处理每个连接
 * - 支持高并发连接
 * - 优雅关闭
 */
public class SmtpServer {
    
    private static final Logger log = LoggerFactory.getLogger(SmtpServer.class);
    
    private final SmtpServerConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    private ServerSocket serverSocket;
    private ExecutorService virtualThreadExecutor;
    
    public SmtpServer(SmtpServerConfig config) {
        this.config = config;
    }
    
    /**
     * 启动 SMTP 服务器
     */
    public void start() throws IOException {
        if (running.get()) {
            log.warn("服务器已在运行中");
            return;
        }
        
        log.info("正在启动 SMTP 服务器...");
        log.info("配置: {}", config);
        
        // 初始化数据库
        DatabaseInitializer.initialize();
        
        // 创建虚拟线程执行器
        // Java 21+ 支持虚拟线程
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("smtp-handler-", 0)
                .factory();
        
        virtualThreadExecutor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        // 创建服务器 Socket
        serverSocket = new ServerSocket(config.getPort());
        running.set(true);
        
        log.info("SMTP 服务器已启动，监听端口: {}", config.getPort());
        log.info("使用虚拟线程处理连接");
        
        // 主循环：接受连接
        acceptConnections();
    }
    
    /**
     * 接受客户端连接
     */
    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // 检查连接数限制
                if (activeConnections.get() >= config.getMaxConnections()) {
                    log.warn("连接数已达上限 ({}), 拒绝新连接: {}",
                            config.getMaxConnections(),
                            clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    continue;
                }
                
                // 设置连接超时
                clientSocket.setSoTimeout(config.getConnectionTimeout());
                
                // 增加活跃连接计数
                activeConnections.incrementAndGet();
                
                // 使用虚拟线程处理连接
                virtualThreadExecutor.submit(() -> {
                    try {
                        new SmtpConnectionHandler(clientSocket, config).run();
                    } finally {
                        activeConnections.decrementAndGet();
                    }
                });
                
            } catch (SocketException e) {
                if (running.get()) {
                    log.error("接受连接时出错: {}", e.getMessage());
                }
                // 如果服务器正在关闭，这是预期的异常
            } catch (IOException e) {
                if (running.get()) {
                    log.error("接受连接时出错: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * 停止 SMTP 服务器
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        log.info("正在停止 SMTP 服务器...");
        running.set(false);
        
        // 关闭服务器 Socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("关闭服务器 Socket 时出错: {}", e.getMessage());
        }
        
        // 关闭虚拟线程执行器
        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdown();
            try {
                if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    virtualThreadExecutor.shutdownNow();
                    log.warn("强制关闭虚拟线程执行器");
                }
            } catch (InterruptedException e) {
                virtualThreadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 关闭数据库连接池
        DatabaseConfig.shutdown();
        
        log.info("SMTP 服务器已停止");
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 获取当前活跃连接数
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    /**
     * 获取服务器配置
     */
    public SmtpServerConfig getConfig() {
        return config;
    }
}

