package com.yhm.pop3.protocol;

/**
 * POP3 响应生成器
 * 
 * 根据 RFC 1939，POP3 响应格式：
 * - 正响应: +OK [message]
 * - 负响应: -ERR [message]
 * 
 * 多行响应以单独的 "." 结束
 */
public final class Pop3Response {
    
    // 响应前缀
    public static final String OK = "+OK";
    public static final String ERR = "-ERR";
    
    private Pop3Response() {
        // 工具类，禁止实例化
    }
    
    // ==================== 正响应 ====================
    
    /**
     * 简单的正响应
     */
    public static String ok() {
        return OK;
    }
    
    /**
     * 带消息的正响应
     */
    public static String ok(String message) {
        return OK + " " + message;
    }
    
    /**
     * 服务就绪响应（欢迎消息）
     * 包含时间戳用于 APOP 认证
     */
    public static String greeting(String domain, String timestamp) {
        return OK + " " + domain + " POP3 server ready " + timestamp;
    }
    
    /**
     * STAT 命令响应
     * +OK nn mm (邮件数量 总大小)
     */
    public static String stat(int messageCount, long totalSize) {
        return OK + " " + messageCount + " " + totalSize;
    }
    
    /**
     * LIST 命令响应（单个邮件）
     * +OK msg size
     */
    public static String listSingle(int messageNumber, long size) {
        return OK + " " + messageNumber + " " + size;
    }
    
    /**
     * LIST 命令响应开始（多个邮件）
     * +OK n messages (m octets)
     */
    public static String listStart(int count, long totalSize) {
        return OK + " " + count + " messages (" + totalSize + " octets)";
    }
    
    /**
     * LIST 项目格式
     * msg size
     */
    public static String listItem(int messageNumber, long size) {
        return messageNumber + " " + size;
    }
    
    /**
     * UIDL 命令响应（单个邮件）
     * +OK msg unique-id
     */
    public static String uidlSingle(int messageNumber, String uniqueId) {
        return OK + " " + messageNumber + " " + uniqueId;
    }
    
    /**
     * UIDL 命令响应开始（多个邮件）
     */
    public static String uidlStart() {
        return OK;
    }
    
    /**
     * UIDL 项目格式
     * msg unique-id
     */
    public static String uidlItem(int messageNumber, String uniqueId) {
        return messageNumber + " " + uniqueId;
    }
    
    /**
     * RETR 命令响应开始
     * +OK size octets
     */
    public static String retrStart(long size) {
        return OK + " " + size + " octets";
    }
    
    /**
     * TOP 命令响应开始
     */
    public static String topStart() {
        return OK;
    }
    
    /**
     * 多行响应结束标记
     */
    public static String multiLineEnd() {
        return ".";
    }
    
    /**
     * 邮件删除成功
     */
    public static String deleted(int messageNumber) {
        return OK + " message " + messageNumber + " deleted";
    }
    
    /**
     * 重置成功
     */
    public static String reset(int restoredCount) {
        return OK + " maildrop has " + restoredCount + " messages";
    }
    
    /**
     * 用户认证成功
     */
    public static String loginSuccess(int messageCount, long totalSize) {
        return OK + " maildrop has " + messageCount + " messages (" + totalSize + " octets)";
    }
    
    /**
     * 关闭连接（正常退出）
     */
    public static String quit(String domain) {
        return OK + " " + domain + " POP3 server signing off";
    }
    
    /**
     * 关闭连接（带更新信息）
     */
    public static String quitWithUpdate(String domain, int deletedCount) {
        return OK + " " + domain + " POP3 server signing off (" + deletedCount + " messages deleted)";
    }
    
    // ==================== 负响应 ====================
    
    /**
     * 简单的错误响应
     */
    public static String err() {
        return ERR;
    }
    
    /**
     * 带消息的错误响应
     */
    public static String err(String message) {
        return ERR + " " + message;
    }
    
    /**
     * 未知命令
     */
    public static String unknownCommand() {
        return ERR + " unknown command";
    }
    
    /**
     * 无效参数
     */
    public static String invalidArgument() {
        return ERR + " invalid argument";
    }
    
    /**
     * 缺少参数
     */
    public static String missingArgument() {
        return ERR + " missing argument";
    }
    
    /**
     * 认证失败
     */
    public static String authFailed() {
        return ERR + " [AUTH] authentication failed";
    }
    
    /**
     * 权限被拒绝（未认证）
     */
    public static String permissionDenied() {
        return ERR + " permission denied";
    }
    
    /**
     * 邮箱已锁定
     */
    public static String maildropLocked() {
        return ERR + " [IN-USE] maildrop already locked";
    }
    
    /**
     * 无效的邮件编号
     */
    public static String noSuchMessage(int messageNumber) {
        return ERR + " no such message, only " + messageNumber + " messages in maildrop";
    }
    
    /**
     * 邮件已删除
     */
    public static String messageDeleted() {
        return ERR + " message already deleted";
    }
    
    /**
     * 服务不可用
     */
    public static String serviceUnavailable() {
        return ERR + " service temporarily unavailable";
    }
    
    /**
     * 命令顺序错误
     */
    public static String badSequence() {
        return ERR + " bad command sequence";
    }
    
    /**
     * 内部错误
     */
    public static String internalError() {
        return ERR + " internal error";
    }
    
    /**
     * 超时
     */
    public static String timeout() {
        return ERR + " connection timeout";
    }
    
    /**
     * 用户名未指定
     */
    public static String userFirst() {
        return ERR + " send USER command first";
    }
    
    /**
     * 已在事务状态
     */
    public static String alreadyAuthenticated() {
        return ERR + " already authenticated";
    }
    
    /**
     * TLS 不可用
     */
    public static String tlsNotAvailable() {
        return ERR + " TLS not available";
    }
    
    /**
     * 用户不存在
     */
    public static String userNotFound() {
        return ERR + " [AUTH] user not found";
    }
}

