package com.yhm.pop3.protocol;

/**
 * POP3 命令枚举
 * 
 * 根据 RFC 1939 定义的 POP3 协议命令
 * 
 * POP3 会话状态：
 * - AUTHORIZATION: 授权状态，等待用户认证
 * - TRANSACTION: 事务状态，处理邮件操作
 * - UPDATE: 更新状态，执行删除并关闭连接
 */
public enum Pop3Command {
    
    // ==================== 授权状态命令 ====================
    
    /**
     * USER 命令 - 指定用户名
     * 语法: USER name
     * 只在 AUTHORIZATION 状态有效
     */
    USER,
    
    /**
     * PASS 命令 - 指定密码
     * 语法: PASS string
     * 只在 USER 命令成功后有效
     */
    PASS,
    
    /**
     * APOP 命令 - 使用摘要认证（可选）
     * 语法: APOP name digest
     * 替代 USER/PASS 的更安全认证方式
     */
    APOP,
    
    // ==================== 事务状态命令 ====================
    
    /**
     * STAT 命令 - 获取邮箱状态
     * 语法: STAT
     * 返回: +OK nn mm (邮件数量和总大小)
     */
    STAT,
    
    /**
     * LIST 命令 - 列出邮件
     * 语法: LIST [msg]
     * 返回邮件列表或指定邮件信息
     */
    LIST,
    
    /**
     * RETR 命令 - 获取邮件内容
     * 语法: RETR msg
     * 返回指定邮件的完整内容
     */
    RETR,
    
    /**
     * DELE 命令 - 标记删除邮件
     * 语法: DELE msg
     * 标记邮件为待删除，在 UPDATE 状态时执行
     */
    DELE,
    
    /**
     * NOOP 命令 - 空操作
     * 语法: NOOP
     * 用于保持连接活跃
     */
    NOOP,
    
    /**
     * RSET 命令 - 重置删除标记
     * 语法: RSET
     * 取消所有 DELE 标记的邮件
     */
    RSET,
    
    /**
     * TOP 命令 - 获取邮件头和部分正文（可选）
     * 语法: TOP msg n
     * 返回邮件头和前 n 行正文
     */
    TOP,
    
    /**
     * UIDL 命令 - 获取邮件唯一标识符（可选）
     * 语法: UIDL [msg]
     * 返回邮件的唯一标识符
     */
    UIDL,
    
    // ==================== 任意状态命令 ====================
    
    /**
     * QUIT 命令 - 退出会话
     * 语法: QUIT
     * 在 TRANSACTION 状态时进入 UPDATE 状态，执行删除操作
     */
    QUIT,
    
    /**
     * CAPA 命令 - 获取服务器能力（扩展）
     * 语法: CAPA
     * 返回服务器支持的扩展功能
     */
    CAPA,
    
    /**
     * STLS 命令 - 启动 TLS 加密（扩展）
     * 语法: STLS
     * 将连接升级为 TLS 加密
     */
    STLS,
    
    /**
     * AUTH 命令 - SASL 认证（扩展）
     * 语法: AUTH mechanism
     * 使用 SASL 机制进行认证
     */
    AUTH,
    
    /**
     * 未知命令
     */
    UNKNOWN;
    
    /**
     * 解析命令字符串
     *
     * @param line 命令行
     * @return 对应的 Pop3Command 枚举
     */
    public static Pop3Command parse(String line) {
        if (line == null || line.isBlank()) {
            return UNKNOWN;
        }
        
        // 获取命令部分（第一个单词）
        String[] parts = line.trim().split("\\s+", 2);
        String command = parts[0].toUpperCase();
        
        return switch (command) {
            case "USER" -> USER;
            case "PASS" -> PASS;
            case "APOP" -> APOP;
            case "STAT" -> STAT;
            case "LIST" -> LIST;
            case "RETR" -> RETR;
            case "DELE" -> DELE;
            case "NOOP" -> NOOP;
            case "RSET" -> RSET;
            case "TOP" -> TOP;
            case "UIDL" -> UIDL;
            case "QUIT" -> QUIT;
            case "CAPA" -> CAPA;
            case "STLS" -> STLS;
            case "AUTH" -> AUTH;
            default -> UNKNOWN;
        };
    }
    
    /**
     * 检查命令是否在授权状态有效
     */
    public boolean isValidInAuthorizationState() {
        return switch (this) {
            case USER, PASS, APOP, QUIT, CAPA, STLS, AUTH -> true;
            default -> false;
        };
    }
    
    /**
     * 检查命令是否在事务状态有效
     */
    public boolean isValidInTransactionState() {
        return switch (this) {
            case STAT, LIST, RETR, DELE, NOOP, RSET, TOP, UIDL, QUIT, CAPA -> true;
            default -> false;
        };
    }
}

