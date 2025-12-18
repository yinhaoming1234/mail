package com.yhm.mail.core.util;

import com.yhm.mail.core.model.Email;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件解析工具类
 * 解析原始邮件内容，提取各个字段
 */
public final class EmailParser {
    
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Za-z-]+):\\s*(.*)$");
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("<([^>]+)>|([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    
    private EmailParser() {
        // 工具类，禁止实例化
    }
    
    /**
     * 解析原始邮件内容
     *
     * @param rawContent 原始邮件内容
     * @param sender     发件人
     * @param recipients 收件人列表
     * @return 解析后的邮件对象
     */
    public static Email parse(String rawContent, String sender, List<String> recipients) {
        String subject = "";
        StringBuilder bodyBuilder = new StringBuilder();
        
        String[] lines = rawContent.split("\r\n|\n");
        boolean inBody = false;
        boolean inHeaders = true;
        
        for (String line : lines) {
            if (inHeaders) {
                if (line.isEmpty()) {
                    inHeaders = false;
                    inBody = true;
                    continue;
                }
                
                Matcher matcher = HEADER_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String headerName = matcher.group(1).toLowerCase();
                    String headerValue = matcher.group(2);
                    
                    if ("subject".equals(headerName)) {
                        subject = decodeHeader(headerValue);
                    }
                }
            } else if (inBody) {
                if (!bodyBuilder.isEmpty()) {
                    bodyBuilder.append("\n");
                }
                bodyBuilder.append(line);
            }
        }
        
        return Email.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipients(new ArrayList<>(recipients))
                .subject(subject)
                .body(bodyBuilder.toString())
                .rawContent(rawContent)
                .size(rawContent.getBytes().length)
                .receivedAt(Instant.now())
                .read(false)
                .deleted(false)
                .build();
    }
    
    /**
     * 提取邮箱地址
     * 从 "Name <email@domain.com>" 或 "email@domain.com" 格式中提取邮箱地址
     *
     * @param input 输入字符串
     * @return 提取的邮箱地址，如果无法提取则返回原字符串
     */
    public static String extractEmailAddress(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        
        Matcher matcher = EMAIL_ADDRESS_PATTERN.matcher(input);
        if (matcher.find()) {
            String bracketed = matcher.group(1);
            return bracketed != null ? bracketed : matcher.group(2);
        }
        return input.trim();
    }
    
    /**
     * 验证邮箱地址格式
     *
     * @param email 邮箱地址
     * @return 是否为有效的邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    /**
     * 提取邮箱的域名部分
     *
     * @param email 邮箱地址
     * @return 域名，如果无法提取则返回空字符串
     */
    public static String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(email.lastIndexOf('@') + 1).toLowerCase();
    }
    
    /**
     * 提取邮箱的本地部分（用户名）
     *
     * @param email 邮箱地址
     * @return 本地部分，如果无法提取则返回空字符串
     */
    public static String extractLocalPart(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(0, email.lastIndexOf('@')).toLowerCase();
    }
    
    /**
     * 解码邮件头部（简单实现，处理 quoted-printable 和 base64）
     *
     * @param header 编码的头部值
     * @return 解码后的值
     */
    private static String decodeHeader(String header) {
        if (header == null) {
            return "";
        }
        
        // 简单实现：处理 =?charset?encoding?encoded_text?= 格式
        if (header.startsWith("=?") && header.contains("?=")) {
            try {
                // 提取编码部分
                String[] parts = header.split("\\?");
                if (parts.length >= 4) {
                    String charset = parts[1];
                    String encoding = parts[2];
                    String encodedText = parts[3];
                    
                    if ("B".equalsIgnoreCase(encoding)) {
                        // Base64 编码
                        return new String(java.util.Base64.getDecoder().decode(encodedText), charset);
                    } else if ("Q".equalsIgnoreCase(encoding)) {
                        // Quoted-Printable 编码
                        return decodeQuotedPrintable(encodedText, charset);
                    }
                }
            } catch (Exception e) {
                // 解码失败，返回原始值
                return header;
            }
        }
        
        return header;
    }
    
    /**
     * 解码 Quoted-Printable 编码的文本
     */
    private static String decodeQuotedPrintable(String text, String charset) {
        StringBuilder result = new StringBuilder();
        byte[] bytes = new byte[text.length()];
        int byteIndex = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '=' && i + 2 < text.length()) {
                try {
                    int hex = Integer.parseInt(text.substring(i + 1, i + 3), 16);
                    bytes[byteIndex++] = (byte) hex;
                    i += 2;
                } catch (NumberFormatException e) {
                    bytes[byteIndex++] = (byte) c;
                }
            } else if (c == '_') {
                bytes[byteIndex++] = ' ';
            } else {
                bytes[byteIndex++] = (byte) c;
            }
        }
        
        try {
            return new String(bytes, 0, byteIndex, charset);
        } catch (Exception e) {
            return text;
        }
    }
}

