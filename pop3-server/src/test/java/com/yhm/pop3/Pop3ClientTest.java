package com.yhm.pop3;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 简单的 POP3 客户端测试
 * 
 * 用于测试 POP3 服务器的基本功能
 */
public class Pop3ClientTest {
    
    private static final String HOST = "localhost";
    private static final int PORT = 1100;
    
    public static void main(String[] args) {
        System.out.println("========== POP3 客户端测试 ==========\n");
        
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            // 设置超时
            socket.setSoTimeout(30000);
            
            // 读取欢迎消息
            String greeting = reader.readLine();
            System.out.println("S: " + greeting);
            
            // 测试 CAPA 命令
            testCommand(reader, writer, "CAPA");
            
            // 测试 USER 命令
            testCommand(reader, writer, "USER test@localhost");
            
            // 测试 PASS 命令
            testCommand(reader, writer, "PASS password123");
            
            // 测试 STAT 命令
            testCommand(reader, writer, "STAT");
            
            // 测试 LIST 命令
            testCommand(reader, writer, "LIST");
            
            // 测试 UIDL 命令
            testCommand(reader, writer, "UIDL");
            
            // 测试 NOOP 命令
            testCommand(reader, writer, "NOOP");
            
            // 测试 RSET 命令
            testCommand(reader, writer, "RSET");
            
            // 测试 QUIT 命令
            testCommand(reader, writer, "QUIT");
            
            System.out.println("\n========== 测试完成 ==========");
            
        } catch (IOException e) {
            System.err.println("连接错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送命令并读取响应
     */
    private static void testCommand(BufferedReader reader, PrintWriter writer, String command) 
            throws IOException {
        
        System.out.println("\nC: " + command);
        writer.println(command);
        
        // 读取响应
        String response = reader.readLine();
        System.out.println("S: " + response);
        
        // 如果是多行响应，继续读取直到遇到单独的点
        if (response != null && response.startsWith("+OK") && isMultiLineResponse(command)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("S: " + line);
                if (".".equals(line)) {
                    break;
                }
            }
        }
    }
    
    /**
     * 检查是否是多行响应命令
     */
    private static boolean isMultiLineResponse(String command) {
        String cmd = command.toUpperCase().split("\\s+")[0];
        return switch (cmd) {
            case "LIST", "UIDL", "RETR", "TOP", "CAPA" -> true;
            default -> false;
        };
    }
}

