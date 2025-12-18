package com.yhm.smtp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 使用 Java 25 虚拟线程的邮件发送器
 * 演示如何使用虚拟线程进行并发邮件发送
 */
public class StructuredMailSender {
    
    private static final Logger log = LoggerFactory.getLogger(StructuredMailSender.class);
    
    private final String smtpHost;
    private final int smtpPort;
    private final int timeout;
    
    public StructuredMailSender(String smtpHost, int smtpPort) {
        this(smtpHost, smtpPort, 30000);
    }
    
    public StructuredMailSender(String smtpHost, int smtpPort, int timeout) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.timeout = timeout;
    }
    
    /**
     * 发送邮件到单个收件人
     */
    public SendResult send(String from, String to, String subject, String body) {
        return send(from, to, subject, body, smtpHost, smtpPort);
    }
    
    /**
     * 使用虚拟线程并行发送邮件到多个收件人
     * 
     * @param from     发件人
     * @param recipients 收件人列表
     * @param subject  主题
     * @param body     正文
     * @return 发送结果列表
     */
    public List<SendResult> sendToMultiple(String from, List<String> recipients, String subject, String body) {
        log.info("使用虚拟线程发送邮件到 {} 个收件人", recipients.size());
        
        List<SendResult> results = new ArrayList<>();
        
        // 使用虚拟线程执行器进行并行发送
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 为每个收件人创建一个任务
            List<Future<SendResult>> futures = new ArrayList<>();
            for (String recipient : recipients) {
                Callable<SendResult> task = () -> send(from, recipient, subject, body);
                futures.add(executor.submit(task));
            }
            
            // 收集结果
            for (Future<SendResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("获取发送结果失败: {}", e.getMessage());
                }
            }
        }
        
        return results;
    }
    
    /**
     * 使用虚拟线程发送邮件，任何一个成功即返回
     * 适用于发送到多个备选服务器的场景
     */
    public SendResult sendToFirstAvailable(String from, String to, String subject, String body, 
                                            List<ServerConfig> servers) {
        log.info("尝试发送邮件到 {} 个服务器中的第一个可用", servers.size());
        
        // 使用虚拟线程执行器
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<SendResult>> futures = new ArrayList<>();
            for (ServerConfig server : servers) {
                Callable<SendResult> task = () -> send(from, to, subject, body, server.host(), server.port());
                futures.add(executor.submit(task));
            }
            
            // 返回第一个成功的结果
            for (Future<SendResult> future : futures) {
                try {
                    SendResult result = future.get();
                    if (result.success()) {
                        // 取消其他任务
                        for (Future<SendResult> f : futures) {
                            f.cancel(true);
                        }
                        return result;
                    }
                } catch (Exception e) {
                    log.debug("服务器不可用: {}", e.getMessage());
                }
            }
        }
        
        return new SendResult(false, "所有服务器都不可用", to);
    }
    
    /**
     * 发送邮件到指定服务器
     */
    private SendResult send(String from, String to, String subject, String body, String host, int port) {
        log.debug("发送邮件: {} -> {} via {}:{}", from, to, host, port);
        
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(timeout);
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            
            // 读取欢迎消息
            String response = reader.readLine();
            if (!response.startsWith("220")) {
                return new SendResult(false, "服务器拒绝连接: " + response, to);
            }
            
            // EHLO
            writer.println("EHLO localhost");
            response = readMultilineResponse(reader);
            if (!response.startsWith("250")) {
                return new SendResult(false, "EHLO 失败: " + response, to);
            }
            
            // MAIL FROM
            writer.println("MAIL FROM:<" + from + ">");
            response = reader.readLine();
            if (!response.startsWith("250")) {
                return new SendResult(false, "MAIL FROM 失败: " + response, to);
            }
            
            // RCPT TO
            writer.println("RCPT TO:<" + to + ">");
            response = reader.readLine();
            if (!response.startsWith("250")) {
                return new SendResult(false, "RCPT TO 失败: " + response, to);
            }
            
            // DATA
            writer.println("DATA");
            response = reader.readLine();
            if (!response.startsWith("354")) {
                return new SendResult(false, "DATA 失败: " + response, to);
            }
            
            // 发送邮件内容
            writer.println("From: " + from);
            writer.println("To: " + to);
            writer.println("Subject: " + subject);
            writer.println("Content-Type: text/plain; charset=UTF-8");
            writer.println();
            writer.println(body);
            writer.println(".");
            
            response = reader.readLine();
            if (!response.startsWith("250")) {
                return new SendResult(false, "邮件发送失败: " + response, to);
            }
            
            // QUIT
            writer.println("QUIT");
            reader.readLine();
            
            log.info("邮件发送成功: {} -> {}", from, to);
            return new SendResult(true, "发送成功", to);
            
        } catch (IOException e) {
            log.error("发送邮件失败: {} -> {} - {}", from, to, e.getMessage());
            return new SendResult(false, "连接错误: " + e.getMessage(), to);
        }
    }
    
    /**
     * 读取多行响应
     */
    private String readMultilineResponse(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            // 检查是否是最后一行（第4个字符是空格而不是减号）
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 发送结果
     */
    public record SendResult(boolean success, String message, String recipient) {}
    
    /**
     * 服务器配置
     */
    public record ServerConfig(String host, int port) {}
}
