package com.yhm.smtp.protocol;

/**
 * SMTP 响应码定义
 * 包含 RFC 5321 定义的标准响应码
 */
public final class SmtpResponse {
    
    private SmtpResponse() {
        // 工具类，禁止实例化
    }
    
    // ==================== 2xx 成功响应 ====================
    
    /**
     * 220 - 服务就绪
     */
    public static String serviceReady(String domain) {
        return "220 " + domain + " ESMTP Service Ready";
    }
    
    /**
     * 221 - 服务关闭传输通道
     */
    public static String serviceClosing(String domain) {
        return "221 " + domain + " Service closing transmission channel";
    }
    
    /**
     * 250 - 请求的邮件操作完成
     */
    public static String ok() {
        return "250 OK";
    }
    
    /**
     * 250 - 带消息的成功响应
     */
    public static String ok(String message) {
        return "250 " + message;
    }
    
    /**
     * 250 - EHLO 响应（多行）
     */
    public static String ehloResponse(String domain, String... extensions) {
        StringBuilder sb = new StringBuilder();
        sb.append("250-").append(domain).append(" Hello");
        for (int i = 0; i < extensions.length; i++) {
            sb.append("\r\n");
            if (i == extensions.length - 1) {
                sb.append("250 ").append(extensions[i]);
            } else {
                sb.append("250-").append(extensions[i]);
            }
        }
        return sb.toString();
    }
    
    // ==================== 3xx 中间响应 ====================
    
    /**
     * 354 - 开始邮件输入
     */
    public static String startMailInput() {
        return "354 Start mail input; end with <CRLF>.<CRLF>";
    }
    
    // ==================== 4xx 临时失败 ====================
    
    /**
     * 421 - 服务不可用
     */
    public static String serviceUnavailable(String domain) {
        return "421 " + domain + " Service not available, closing transmission channel";
    }
    
    /**
     * 450 - 请求的邮件操作未完成：邮箱不可用
     */
    public static String mailboxUnavailable() {
        return "450 Requested mail action not taken: mailbox unavailable";
    }
    
    /**
     * 451 - 请求的操作中止：本地错误
     */
    public static String localError() {
        return "451 Requested action aborted: local error in processing";
    }
    
    /**
     * 452 - 系统存储不足
     */
    public static String insufficientStorage() {
        return "452 Requested action not taken: insufficient system storage";
    }
    
    // ==================== 5xx 永久失败 ====================
    
    /**
     * 500 - 语法错误，命令无法识别
     */
    public static String syntaxError() {
        return "500 Syntax error, command unrecognized";
    }
    
    /**
     * 500 - 语法错误，带消息
     */
    public static String syntaxError(String message) {
        return "500 Syntax error: " + message;
    }
    
    /**
     * 501 - 参数语法错误
     */
    public static String parameterSyntaxError() {
        return "501 Syntax error in parameters or arguments";
    }
    
    /**
     * 502 - 命令未实现
     */
    public static String commandNotImplemented() {
        return "502 Command not implemented";
    }
    
    /**
     * 503 - 错误的命令序列
     */
    public static String badSequence() {
        return "503 Bad sequence of commands";
    }
    
    /**
     * 504 - 命令参数未实现
     */
    public static String parameterNotImplemented() {
        return "504 Command parameter not implemented";
    }
    
    /**
     * 550 - 请求的操作未执行：邮箱不可用
     */
    public static String mailboxNotFound() {
        return "550 Requested action not taken: mailbox unavailable";
    }
    
    /**
     * 550 - 用户不存在
     */
    public static String userNotFound(String email) {
        return "550 User " + email + " not found";
    }
    
    /**
     * 551 - 用户不在本地
     */
    public static String userNotLocal(String forwardPath) {
        return "551 User not local; please try " + forwardPath;
    }
    
    /**
     * 552 - 超出存储分配
     */
    public static String storageExceeded() {
        return "552 Requested mail action aborted: exceeded storage allocation";
    }
    
    /**
     * 553 - 邮箱名称不允许
     */
    public static String mailboxNameNotAllowed() {
        return "553 Requested action not taken: mailbox name not allowed";
    }
    
    /**
     * 554 - 事务失败
     */
    public static String transactionFailed() {
        return "554 Transaction failed";
    }
    
    /**
     * 554 - 带消息的事务失败
     */
    public static String transactionFailed(String message) {
        return "554 Transaction failed: " + message;
    }
}

