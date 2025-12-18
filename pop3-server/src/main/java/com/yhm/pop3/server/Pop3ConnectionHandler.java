package com.yhm.pop3.server;

import com.yhm.pop3.config.Pop3ServerConfig;
import com.yhm.pop3.db.Pop3EmailRepository;
import com.yhm.pop3.protocol.Pop3CommandHandler;
import com.yhm.pop3.protocol.Pop3Response;
import com.yhm.pop3.protocol.Pop3Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * POP3 连接处理器
 * 
 * 处理单个客户端连接的 POP3 会话
 * 设计为在虚拟线程中运行
 * 
 * Java 25 特性：
 * - 使用虚拟线程处理每个连接
 * - 利用 record 和模式匹配简化代码
 */
public class Pop3ConnectionHandler implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(Pop3ConnectionHandler.class);
    
    private final Socket socket;
    private final Pop3ServerConfig config;
    private final Pop3CommandHandler commandHandler;
    private final Pop3Session session;
    private final Pop3EmailRepository emailRepository;
    
    public Pop3ConnectionHandler(Socket socket, Pop3ServerConfig config) {
        this.socket = socket;
        this.config = config;
        this.commandHandler = new Pop3CommandHandler(config);
        this.session = new Pop3Session(socket.getRemoteSocketAddress().toString());
        this.emailRepository = new Pop3EmailRepository();
    }
    
    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        log.info("新 POP3 连接: {} (虚拟线程: {}, 线程名: {})",
                session.getRemoteAddress(),
                currentThread.isVirtual(),
                currentThread.getName());
        
        try {
            socket.setSoTimeout(config.getReadTimeout());
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                
                // 发送欢迎消息（包含 APOP 时间戳）
                sendResponse(writer, Pop3Response.greeting(
                        config.getDomain(), session.getApopTimestamp()));
                
                // 主循环：读取并处理命令
                String line;
                while ((line = readLine(reader)) != null) {
                    log.debug("收到: {}", maskPassword(line));
                    
                    // 处理命令
                    String response = commandHandler.handleCommand(line, session);
                    
                    if (response != null) {
                        sendResponse(writer, response);
                    }
                    
                    // 检查是否应该断开连接
                    if (session.isInUpdateState()) {
                        break;
                    }
                    
                    // 检查认证失败次数
                    if (session.getAuthFailedAttempts() >= config.getAuthFailedMaxAttempts()) {
                        log.warn("认证失败次数过多，断开连接: {}", session.getRemoteAddress());
                        break;
                    }
                }
                
            }
            
        } catch (SocketTimeoutException e) {
            log.warn("POP3 连接超时: {}", session.getRemoteAddress());
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                sendResponse(writer, Pop3Response.timeout());
            } catch (IOException ignored) {
                // 忽略关闭时的错误
            }
        } catch (IOException e) {
            log.error("POP3 连接错误: {} - {}", session.getRemoteAddress(), e.getMessage());
        } finally {
            cleanup();
            closeSocket();
            log.info("POP3 连接关闭: {}", session.getRemoteAddress());
        }
    }
    
    /**
     * 读取一行数据
     * 处理 CRLF 行结束符
     */
    private String readLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            // 移除可能的 CR 字符
            line = line.stripTrailing();
        }
        return line;
    }
    
    /**
     * 发送响应
     */
    private void sendResponse(PrintWriter writer, String response) {
        // 对于多行响应，直接发送
        if (response.contains("\r\n")) {
            // 已经包含换行符的多行响应
            String[] lines = response.split("\r\n");
            for (String line : lines) {
                writer.println(line);
                log.debug("发送: {}", line);
            }
        } else {
            writer.println(response);
            log.debug("发送: {}", response);
        }
    }
    
    /**
     * 掩码密码（用于日志）
     */
    private String maskPassword(String line) {
        if (line.toUpperCase().startsWith("PASS ")) {
            return "PASS ********";
        }
        return line;
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        // 如果会话已认证但没有正常退出，需要解锁邮箱
        if (session.isInTransactionState() && session.getUsername() != null) {
            try {
                emailRepository.unlockMaildrop(session.getUsername());
                log.debug("异常断开，已解锁邮箱: {}", session.getUsername());
            } catch (Exception e) {
                log.error("解锁邮箱失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 关闭 Socket
     */
    private void closeSocket() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.debug("关闭 socket 时出错: {}", e.getMessage());
        }
    }
}

