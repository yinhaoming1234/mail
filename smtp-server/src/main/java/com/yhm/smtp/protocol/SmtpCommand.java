package com.yhm.smtp.protocol;

/**
 * SMTP 命令枚举
 * 定义了 SMTP 协议支持的所有命令
 */
public enum SmtpCommand {
    
    /**
     * HELO - 初始握手命令（基本 SMTP）
     */
    HELO("HELO"),
    
    /**
     * EHLO - 扩展握手命令（ESMTP）
     */
    EHLO("EHLO"),
    
    /**
     * MAIL FROM - 指定发件人
     */
    MAIL("MAIL"),
    
    /**
     * RCPT TO - 指定收件人
     */
    RCPT("RCPT"),
    
    /**
     * DATA - 开始邮件数据传输
     */
    DATA("DATA"),
    
    /**
     * RSET - 重置会话状态
     */
    RSET("RSET"),
    
    /**
     * NOOP - 空操作（保持连接）
     */
    NOOP("NOOP"),
    
    /**
     * QUIT - 结束会话
     */
    QUIT("QUIT"),
    
    /**
     * VRFY - 验证用户/邮箱
     */
    VRFY("VRFY"),
    
    /**
     * EXPN - 展开邮件列表
     */
    EXPN("EXPN"),
    
    /**
     * HELP - 获取帮助信息
     */
    HELP("HELP"),
    
    /**
     * AUTH - 认证命令（ESMTP 扩展）
     */
    AUTH("AUTH"),
    
    /**
     * STARTTLS - 开始 TLS 加密（ESMTP 扩展）
     */
    STARTTLS("STARTTLS"),
    
    /**
     * 未知命令
     */
    UNKNOWN("UNKNOWN");
    
    private final String name;
    
    SmtpCommand(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 根据命令字符串解析命令类型
     *
     * @param command 命令字符串
     * @return 对应的 SmtpCommand 枚举值
     */
    public static SmtpCommand parse(String command) {
        if (command == null || command.isBlank()) {
            return UNKNOWN;
        }
        
        String upperCommand = command.toUpperCase().trim();
        
        // 处理带参数的命令
        if (upperCommand.startsWith("MAIL ")) {
            return MAIL;
        }
        if (upperCommand.startsWith("RCPT ")) {
            return RCPT;
        }
        
        for (SmtpCommand cmd : values()) {
            if (cmd.name.equals(upperCommand) || upperCommand.startsWith(cmd.name + " ")) {
                return cmd;
            }
        }
        
        return UNKNOWN;
    }
}

