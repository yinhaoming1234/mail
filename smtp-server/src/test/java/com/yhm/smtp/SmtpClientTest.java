package com.yhm.smtp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 简单的 SMTP 客户端测试
 * 用于测试 SMTP 服务器
 * 
 * 使用方法：
 * 1. 启动 SMTP 服务器
 * 2. 运行此测试程序
 */
public class SmtpClientTest {
    
    private final String host;
    private final int port;
    
    public SmtpClientTest(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2525;
        
        SmtpClientTest client = new SmtpClientTest(host, port);
        
        System.out.println("==========================================");
        System.out.println("       SMTP 客户端测试程序");
        System.out.println("==========================================");
        System.out.println("连接到: " + host + ":" + port);
        System.out.println();
        
        try {
            // 测试基本的 SMTP 交互
            client.testBasicSmtp();
            
            // 测试发送邮件
            client.testSendEmail();
            
            // 测试错误处理
            client.testErrorHandling();
            
            System.out.println("\n所有测试完成！");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试基本的 SMTP 握手
     */
    public void testBasicSmtp() throws IOException {
        System.out.println("=== 测试 1: 基本 SMTP 握手 ===");
        
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            socket.setSoTimeout(10000);
            
            // 读取欢迎消息
            String response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("220") : "期望 220 响应";
            
            // 发送 EHLO
            System.out.println("C: EHLO test.client.com");
            writer.println("EHLO test.client.com");
            
            // 读取多行响应
            while ((response = reader.readLine()) != null) {
                System.out.println("S: " + response);
                if (response.startsWith("250 ")) break;  // 最后一行
            }
            
            // 发送 QUIT
            System.out.println("C: QUIT");
            writer.println("QUIT");
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("221") : "期望 221 响应";
        }
        
        System.out.println("测试 1 通过\n");
    }
    
    /**
     * 测试发送邮件
     */
    public void testSendEmail() throws IOException {
        System.out.println("=== 测试 2: 发送邮件 ===");
        
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            socket.setSoTimeout(10000);
            
            // 读取欢迎消息
            String response = reader.readLine();
            System.out.println("S: " + response);
            
            // EHLO
            System.out.println("C: EHLO test.client.com");
            writer.println("EHLO test.client.com");
            while ((response = reader.readLine()) != null) {
                System.out.println("S: " + response);
                if (response.startsWith("250 ")) break;
            }
            
            // MAIL FROM
            System.out.println("C: MAIL FROM:<sender@example.com>");
            writer.println("MAIL FROM:<sender@example.com>");
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("250") : "期望 250 响应";
            
            // RCPT TO
            System.out.println("C: RCPT TO:<test@localhost>");
            writer.println("RCPT TO:<test@localhost>");
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("250") : "期望 250 响应";
            
            // DATA
            System.out.println("C: DATA");
            writer.println("DATA");
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("354") : "期望 354 响应";
            
            // 邮件内容
            System.out.println("C: [发送邮件内容]");
            writer.println("From: sender@example.com");
            writer.println("To: test@localhost");
            writer.println("Subject: Test Email from Java 25 SMTP Client");
            writer.println("Date: " + java.time.OffsetDateTime.now());
            writer.println("Content-Type: text/plain; charset=UTF-8");
            writer.println();
            writer.println("这是一封测试邮件。");
            writer.println("使用 Java 25 虚拟线程实现的 SMTP 服务器。");
            writer.println(".");
            
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("250") : "期望 250 响应";
            
            // QUIT
            System.out.println("C: QUIT");
            writer.println("QUIT");
            response = reader.readLine();
            System.out.println("S: " + response);
        }
        
        System.out.println("测试 2 通过\n");
    }
    
    /**
     * 测试错误处理
     */
    public void testErrorHandling() throws IOException {
        System.out.println("=== 测试 3: 错误处理 ===");
        
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            socket.setSoTimeout(10000);
            
            // 读取欢迎消息
            reader.readLine();
            
            // 测试无效命令
            System.out.println("C: INVALID_COMMAND");
            writer.println("INVALID_COMMAND");
            String response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("500") : "期望 500 响应（无效命令）";
            
            // EHLO
            writer.println("EHLO test.client.com");
            while ((response = reader.readLine()) != null) {
                if (response.startsWith("250 ")) break;
            }
            
            // 测试错误的命令序列（未设置 MAIL FROM 就发送 DATA）
            System.out.println("C: DATA (without MAIL FROM)");
            writer.println("DATA");
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("503") : "期望 503 响应（错误序列）";
            
            // 测试发送到不存在的用户
            writer.println("MAIL FROM:<sender@example.com>");
            reader.readLine();
            
            System.out.println("C: RCPT TO:<nonexistent@localhost>");
            writer.println("RCPT TO:<nonexistent@localhost>");
            response = reader.readLine();
            System.out.println("S: " + response);
            assert response.startsWith("550") : "期望 550 响应（用户不存在）";
            
            // QUIT
            writer.println("QUIT");
            reader.readLine();
        }
        
        System.out.println("测试 3 通过\n");
    }
}

