package com.yhm.pop3.server;

import com.yhm.pop3.config.Pop3ServerConfig;
import com.yhm.pop3.db.DatabaseConfig;
import com.yhm.pop3.db.DatabaseInitializer;
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
 * POP3 服务器
 * 
 * 使用 Java 25 虚拟线程处理并发连接
 * 
 * 主要特性：
 * - 使用虚拟线程（Virtual Threads）处理每个连接
 * - 支持高并发连接
 * - 优雅关闭
 * - 自动超时断开
 * 
 * Java 25 特性：
 * - 虚拟线程 (Virtual Threads) - 轻量级线程，适合 I/O 密集型任务
 * - ScopedValue - 用于传递请求上下文
 * - 结构化并发 (Structured Concurrency) - 管理并发任务生命周期
 */
public class Pop3Server {
    
    private static final Logger log = LoggerFactory.getLogger(Pop3Server.class);
    
    private final Pop3ServerConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    private ServerSocket serverSocket;
    private ExecutorService virtualThreadExecutor;
    
    public Pop3Server(Pop3ServerConfig config) {
        this.config = config;
    }
    
    /**
     * 启动 POP3 服务器
     */
    public void start() throws IOException {
        if (running.get()) {
            log.warn("服务器已在运行中");
            return;
        }
        
        log.info("正在启动 POP3 服务器...");
        log.info("配置: {}", config);
        
        // 初始化数据库
        DatabaseInitializer.initialize();
        
        // 创建虚拟线程执行器
        // Java 21+ 支持虚拟线程
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("pop3-handler-", 0)
                .factory();
        
        virtualThreadExecutor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        // 创建服务器 Socket
        serverSocket = new ServerSocket(config.getPort());
        running.set(true);
        
        log.info("POP3 服务器已启动，监听端口: {}", config.getPort());
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
                    sendRejectResponse(clientSocket);
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
                        new Pop3ConnectionHandler(clientSocket, config).run();
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
     * 发送拒绝响应
     */
    private void sendRejectResponse(Socket socket) {
        try {
            socket.getOutputStream().write(
                    "-ERR server too busy, try again later\r\n".getBytes());
            socket.getOutputStream().flush();
        } catch (IOException e) {
            log.debug("发送拒绝响应失败: {}", e.getMessage());
        }
    }
    
    /**
     * 停止 POP3 服务器
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        log.info("正在停止 POP3 服务器...");
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
        
        log.info("POP3 服务器已停止");
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
    public Pop3ServerConfig getConfig() {
        return config;
    }
}

