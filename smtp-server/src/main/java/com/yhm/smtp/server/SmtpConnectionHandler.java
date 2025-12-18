package com.yhm.smtp.server;

import com.yhm.smtp.config.SmtpServerConfig;
import com.yhm.smtp.protocol.SmtpCommandHandler;
import com.yhm.smtp.protocol.SmtpResponse;
import com.yhm.smtp.protocol.SmtpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * SMTP 连接处理器
 * 处理单个客户端连接的 SMTP 会话
 * 设计为在虚拟线程中运行
 */
public class SmtpConnectionHandler implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(SmtpConnectionHandler.class);
    
    private final Socket socket;
    private final SmtpServerConfig config;
    private final SmtpCommandHandler commandHandler;
    private final SmtpSession session;
    
    public SmtpConnectionHandler(Socket socket, SmtpServerConfig config) {
        this.socket = socket;
        this.config = config;
        this.commandHandler = new SmtpCommandHandler(config);
        this.session = new SmtpSession(socket.getRemoteSocketAddress().toString());
    }
    
    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        log.info("新连接: {} (虚拟线程: {}, 线程名: {})",
                session.getRemoteAddress(),
                currentThread.isVirtual(),
                currentThread.getName());
        
        try {
            socket.setSoTimeout(config.getReadTimeout());
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                
                // 发送欢迎消息
                sendResponse(writer, SmtpResponse.serviceReady(config.getDomain()));
                
                // 主循环：读取并处理命令
                String line;
                while ((line = readLine(reader)) != null) {
                    log.debug("收到: {}", line);
                    
                    // 处理命令
                    String response = commandHandler.handleCommand(line, session);
                    
                    // 如果在 DATA 状态且返回 null，表示继续接收数据
                    if (response != null) {
                        sendResponse(writer, response);
                    }
                    
                    // 检查是否应该断开连接
                    if (session.getState() == SmtpSession.State.QUIT) {
                        break;
                    }
                }
                
            }
            
        } catch (SocketTimeoutException e) {
            log.warn("连接超时: {}", session.getRemoteAddress());
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                sendResponse(writer, SmtpResponse.serviceUnavailable(config.getDomain()));
            } catch (IOException ignored) {
                // 忽略关闭时的错误
            }
        } catch (IOException e) {
            log.error("连接错误: {} - {}", session.getRemoteAddress(), e.getMessage());
        } finally {
            closeSocket();
            log.info("连接关闭: {}", session.getRemoteAddress());
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
     * 确保以 CRLF 结束
     */
    private void sendResponse(PrintWriter writer, String response) {
        log.debug("发送: {}", response);
        // PrintWriter 的 println 会自动添加行结束符
        // 对于多行响应，直接发送（已包含 CRLF）
        if (response.contains("\r\n")) {
            writer.print(response + "\r\n");
            writer.flush();
        } else {
            writer.println(response);
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

