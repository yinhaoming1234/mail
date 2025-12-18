package com.yhm.pop3.protocol;

import com.yhm.mail.core.model.Email;
import com.yhm.pop3.config.Pop3ServerConfig;
import com.yhm.pop3.db.Pop3EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * POP3 命令处理器
 * 
 * 处理所有 POP3 协议命令，返回响应
 * 
 * 实现 RFC 1939 定义的 POP3 协议
 */
public class Pop3CommandHandler {
    
    private static final Logger log = LoggerFactory.getLogger(Pop3CommandHandler.class);
    
    private final Pop3ServerConfig config;
    private final Pop3EmailRepository emailRepository;
    
    public Pop3CommandHandler(Pop3ServerConfig config) {
        this.config = config;
        this.emailRepository = new Pop3EmailRepository();
    }
    
    /**
     * 处理 POP3 命令
     *
     * @param line    原始命令行
     * @param session 当前会话
     * @return 响应字符串（可能是多行）
     */
    public String handleCommand(String line, Pop3Session session) {
        if (line == null || line.isBlank()) {
            return Pop3Response.unknownCommand();
        }
        
        // 更新活动时间
        session.updateLastActivityTime();
        
        Pop3Command command = Pop3Command.parse(line);
        
        // 检查命令在当前状态是否有效
        if (session.isInAuthorizationState() && !command.isValidInAuthorizationState()) {
            return Pop3Response.permissionDenied();
        }
        
        if (session.isInTransactionState() && !command.isValidInTransactionState()) {
            return Pop3Response.badSequence();
        }
        
        return switch (command) {
            case USER -> handleUser(line, session);
            case PASS -> handlePass(line, session);
            case APOP -> handleApop(line, session);
            case STAT -> handleStat(session);
            case LIST -> handleList(line, session);
            case RETR -> handleRetr(line, session);
            case DELE -> handleDele(line, session);
            case NOOP -> handleNoop();
            case RSET -> handleRset(session);
            case TOP -> handleTop(line, session);
            case UIDL -> handleUidl(line, session);
            case QUIT -> handleQuit(session);
            case CAPA -> handleCapa(session);
            case STLS -> Pop3Response.tlsNotAvailable();
            case AUTH -> Pop3Response.err("AUTH not supported, use USER/PASS");
            case UNKNOWN -> Pop3Response.unknownCommand();
        };
    }
    
    // ==================== 授权状态命令 ====================
    
    /**
     * 处理 USER 命令
     * USER username
     */
    private String handleUser(String line, Pop3Session session) {
        if (!session.isInAuthorizationState()) {
            return Pop3Response.alreadyAuthenticated();
        }
        
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return Pop3Response.missingArgument();
        }
        
        String username = parts[1].trim();
        
        // 验证用户是否存在
        try {
            if (!emailRepository.userExists(username)) {
                log.warn("用户不存在: {}", username);
                return Pop3Response.userNotFound();
            }
        } catch (SQLException e) {
            log.error("检查用户时数据库错误: {}", e.getMessage());
            return Pop3Response.internalError();
        }
        
        session.setUsername(username);
        session.setUserCommandReceived(true);
        
        log.debug("USER: {}", username);
        
        return Pop3Response.ok("user accepted");
    }
    
    /**
     * 处理 PASS 命令
     * PASS password
     */
    private String handlePass(String line, Pop3Session session) {
        if (!session.isInAuthorizationState()) {
            return Pop3Response.alreadyAuthenticated();
        }
        
        if (!session.isUserCommandReceived()) {
            return Pop3Response.userFirst();
        }
        
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2) {
            return Pop3Response.missingArgument();
        }
        
        String password = parts[1];
        
        try {
            // 验证密码
            boolean authenticated = emailRepository.authenticateUser(
                    session.getUsername(), password);
            
            if (!authenticated) {
                session.incrementAuthFailedAttempts();
                log.warn("认证失败: {} (尝试次数: {})",
                        session.getUsername(), session.getAuthFailedAttempts());
                
                // 检查是否超过最大尝试次数
                if (session.getAuthFailedAttempts() >= config.getAuthFailedMaxAttempts()) {
                    return Pop3Response.err("[AUTH] too many failed attempts");
                }
                
                session.setUserCommandReceived(false);
                return Pop3Response.authFailed();
            }
            
            // 认证成功，加载邮件
            return loadMaildropAndTransition(session);
            
        } catch (SQLException e) {
            log.error("认证时数据库错误: {}", e.getMessage());
            return Pop3Response.internalError();
        }
    }
    
    /**
     * 处理 APOP 命令
     * APOP username digest
     */
    private String handleApop(String line, Pop3Session session) {
        if (!session.isInAuthorizationState()) {
            return Pop3Response.alreadyAuthenticated();
        }
        
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 3) {
            return Pop3Response.missingArgument();
        }
        
        String username = parts[1];
        String clientDigest = parts[2].toLowerCase();
        
        try {
            // 获取用户密码
            String password = emailRepository.getUserPassword(username);
            if (password == null) {
                session.incrementAuthFailedAttempts();
                return Pop3Response.userNotFound();
            }
            
            // 计算预期的摘要并比较
            String expectedDigest = session.computeApopDigest(password);
            
            if (!expectedDigest.equalsIgnoreCase(clientDigest)) {
                session.incrementAuthFailedAttempts();
                log.warn("APOP 认证失败: {}", username);
                return Pop3Response.authFailed();
            }
            
            session.setUsername(username);
            return loadMaildropAndTransition(session);
            
        } catch (SQLException e) {
            log.error("APOP 认证时数据库错误: {}", e.getMessage());
            return Pop3Response.internalError();
        }
    }
    
    /**
     * 加载邮件箱并转换到事务状态
     */
    private String loadMaildropAndTransition(Pop3Session session) throws SQLException {
        // 检查邮箱是否被锁定（其他会话正在使用）
        if (emailRepository.isMaildropLocked(session.getUsername())) {
            return Pop3Response.maildropLocked();
        }
        
        // 锁定邮箱
        emailRepository.lockMaildrop(session.getUsername());
        
        // 加载邮件
        List<Email> emails = emailRepository.findByOwner(session.getUsername());
        session.loadMaildrop(emails);
        
        // 转换到事务状态
        session.setState(Pop3Session.State.TRANSACTION);
        session.resetAuthFailedAttempts();
        
        log.info("用户 {} 登录成功，邮件数: {}, 总大小: {}",
                session.getUsername(), session.getMessageCount(), session.getTotalSize());
        
        return Pop3Response.loginSuccess(session.getMessageCount(), session.getTotalSize());
    }
    
    // ==================== 事务状态命令 ====================
    
    /**
     * 处理 STAT 命令
     * 返回邮箱状态：邮件数量和总大小
     */
    private String handleStat(Pop3Session session) {
        return Pop3Response.stat(session.getMessageCount(), session.getTotalSize());
    }
    
    /**
     * 处理 LIST 命令
     * LIST [msg]
     */
    private String handleList(String line, Pop3Session session) {
        String[] parts = line.split("\\s+", 2);
        
        if (parts.length == 1) {
            // 列出所有邮件
            return buildListAllResponse(session);
        } else {
            // 列出指定邮件
            try {
                int msgNum = Integer.parseInt(parts[1].trim());
                return buildListSingleResponse(session, msgNum);
            } catch (NumberFormatException e) {
                return Pop3Response.invalidArgument();
            }
        }
    }
    
    private String buildListAllResponse(Pop3Session session) {
        StringBuilder response = new StringBuilder();
        response.append(Pop3Response.listStart(session.getMessageCount(), session.getTotalSize()));
        
        for (Pop3Session.MaildropMessage msg : session.getActiveMessages()) {
            response.append("\r\n");
            response.append(Pop3Response.listItem(msg.messageNumber(), msg.getSize()));
        }
        
        response.append("\r\n");
        response.append(Pop3Response.multiLineEnd());
        
        return response.toString();
    }
    
    private String buildListSingleResponse(Pop3Session session, int msgNum) {
        var msgOpt = session.getMessage(msgNum);
        
        if (msgOpt.isEmpty()) {
            return Pop3Response.noSuchMessage(session.getMessageCount());
        }
        
        var msg = msgOpt.get();
        if (msg.deleted()) {
            return Pop3Response.messageDeleted();
        }
        
        return Pop3Response.listSingle(msg.messageNumber(), msg.getSize());
    }
    
    /**
     * 处理 RETR 命令
     * RETR msg
     */
    private String handleRetr(String line, Pop3Session session) {
        String[] parts = line.split("\\s+", 2);
        
        if (parts.length < 2) {
            return Pop3Response.missingArgument();
        }
        
        try {
            int msgNum = Integer.parseInt(parts[1].trim());
            return buildRetrResponse(session, msgNum);
        } catch (NumberFormatException e) {
            return Pop3Response.invalidArgument();
        }
    }
    
    private String buildRetrResponse(Pop3Session session, int msgNum) {
        var msgOpt = session.getMessage(msgNum);
        
        if (msgOpt.isEmpty()) {
            return Pop3Response.noSuchMessage(session.getMessageCount());
        }
        
        var msg = msgOpt.get();
        if (msg.deleted()) {
            return Pop3Response.messageDeleted();
        }
        
        Email email = msg.email();
        String content = email.getRawContent();
        
        StringBuilder response = new StringBuilder();
        response.append(Pop3Response.retrStart(msg.getSize()));
        response.append("\r\n");
        
        // 字节填充：以点开头的行需要添加额外的点
        for (String contentLine : content.split("\r\n|\n")) {
            if (contentLine.startsWith(".")) {
                response.append(".");
            }
            response.append(contentLine);
            response.append("\r\n");
        }
        
        response.append(Pop3Response.multiLineEnd());
        
        return response.toString();
    }
    
    /**
     * 处理 DELE 命令
     * DELE msg
     */
    private String handleDele(String line, Pop3Session session) {
        String[] parts = line.split("\\s+", 2);
        
        if (parts.length < 2) {
            return Pop3Response.missingArgument();
        }
        
        try {
            int msgNum = Integer.parseInt(parts[1].trim());
            
            var msgOpt = session.getMessage(msgNum);
            if (msgOpt.isEmpty()) {
                return Pop3Response.noSuchMessage(session.getMessageCount());
            }
            
            var msg = msgOpt.get();
            if (msg.deleted()) {
                return Pop3Response.messageDeleted();
            }
            
            session.markDeleted(msgNum);
            log.debug("邮件 {} 标记为删除", msgNum);
            
            return Pop3Response.deleted(msgNum);
            
        } catch (NumberFormatException e) {
            return Pop3Response.invalidArgument();
        }
    }
    
    /**
     * 处理 NOOP 命令
     */
    private String handleNoop() {
        return Pop3Response.ok();
    }
    
    /**
     * 处理 RSET 命令
     */
    private String handleRset(Pop3Session session) {
        int restored = session.resetDeletedFlags();
        log.debug("重置删除标记，恢复了 {} 封邮件", restored);
        return Pop3Response.reset(session.getMessageCount());
    }
    
    /**
     * 处理 TOP 命令
     * TOP msg n
     */
    private String handleTop(String line, Pop3Session session) {
        String[] parts = line.split("\\s+", 3);
        
        if (parts.length < 3) {
            return Pop3Response.missingArgument();
        }
        
        try {
            int msgNum = Integer.parseInt(parts[1].trim());
            int lines = Integer.parseInt(parts[2].trim());
            
            if (lines < 0) {
                return Pop3Response.invalidArgument();
            }
            
            return buildTopResponse(session, msgNum, lines);
            
        } catch (NumberFormatException e) {
            return Pop3Response.invalidArgument();
        }
    }
    
    private String buildTopResponse(Pop3Session session, int msgNum, int lines) {
        var msgOpt = session.getMessage(msgNum);
        
        if (msgOpt.isEmpty()) {
            return Pop3Response.noSuchMessage(session.getMessageCount());
        }
        
        var msg = msgOpt.get();
        if (msg.deleted()) {
            return Pop3Response.messageDeleted();
        }
        
        Email email = msg.email();
        String content = email.getRawContent();
        
        StringBuilder response = new StringBuilder();
        response.append(Pop3Response.topStart());
        response.append("\r\n");
        
        // 分离头部和正文
        String[] contentParts = content.split("\r\n\r\n|\n\n", 2);
        String headers = contentParts[0];
        String body = contentParts.length > 1 ? contentParts[1] : "";
        
        // 添加头部
        for (String headerLine : headers.split("\r\n|\n")) {
            if (headerLine.startsWith(".")) {
                response.append(".");
            }
            response.append(headerLine);
            response.append("\r\n");
        }
        
        // 空行分隔头部和正文
        response.append("\r\n");
        
        // 添加指定行数的正文
        if (lines > 0 && !body.isEmpty()) {
            String[] bodyLines = body.split("\r\n|\n");
            int linesToSend = Math.min(lines, bodyLines.length);
            
            for (int i = 0; i < linesToSend; i++) {
                String bodyLine = bodyLines[i];
                if (bodyLine.startsWith(".")) {
                    response.append(".");
                }
                response.append(bodyLine);
                response.append("\r\n");
            }
        }
        
        response.append(Pop3Response.multiLineEnd());
        
        return response.toString();
    }
    
    /**
     * 处理 UIDL 命令
     * UIDL [msg]
     */
    private String handleUidl(String line, Pop3Session session) {
        String[] parts = line.split("\\s+", 2);
        
        if (parts.length == 1) {
            // 列出所有邮件的 UIDL
            return buildUidlAllResponse(session);
        } else {
            // 列出指定邮件的 UIDL
            try {
                int msgNum = Integer.parseInt(parts[1].trim());
                return buildUidlSingleResponse(session, msgNum);
            } catch (NumberFormatException e) {
                return Pop3Response.invalidArgument();
            }
        }
    }
    
    private String buildUidlAllResponse(Pop3Session session) {
        StringBuilder response = new StringBuilder();
        response.append(Pop3Response.uidlStart());
        
        for (Pop3Session.MaildropMessage msg : session.getActiveMessages()) {
            response.append("\r\n");
            response.append(Pop3Response.uidlItem(msg.messageNumber(), msg.getUniqueId()));
        }
        
        response.append("\r\n");
        response.append(Pop3Response.multiLineEnd());
        
        return response.toString();
    }
    
    private String buildUidlSingleResponse(Pop3Session session, int msgNum) {
        var msgOpt = session.getMessage(msgNum);
        
        if (msgOpt.isEmpty()) {
            return Pop3Response.noSuchMessage(session.getMessageCount());
        }
        
        var msg = msgOpt.get();
        if (msg.deleted()) {
            return Pop3Response.messageDeleted();
        }
        
        return Pop3Response.uidlSingle(msg.messageNumber(), msg.getUniqueId());
    }
    
    /**
     * 处理 QUIT 命令
     */
    private String handleQuit(Pop3Session session) {
        if (session.isInTransactionState()) {
            // 进入 UPDATE 状态
            session.setState(Pop3Session.State.UPDATE);
            
            // 执行删除操作
            int deletedCount = 0;
            try {
                deletedCount = commitDeletions(session);
            } catch (SQLException e) {
                log.error("执行删除操作时出错: {}", e.getMessage());
            }
            
            // 解锁邮箱
            try {
                emailRepository.unlockMaildrop(session.getUsername());
            } catch (SQLException e) {
                log.error("解锁邮箱时出错: {}", e.getMessage());
            }
            
            log.info("用户 {} 退出，删除了 {} 封邮件", session.getUsername(), deletedCount);
            
            return Pop3Response.quitWithUpdate(config.getDomain(), deletedCount);
        }
        
        return Pop3Response.quit(config.getDomain());
    }
    
    /**
     * 提交删除操作
     */
    private int commitDeletions(Pop3Session session) throws SQLException {
        int deleted = 0;
        
        for (Pop3Session.MaildropMessage msg : session.getDeletedMessages()) {
            if (emailRepository.markAsDeleted(msg.email().getId())) {
                deleted++;
            }
        }
        
        return deleted;
    }
    
    /**
     * 处理 CAPA 命令
     */
    private String handleCapa(Pop3Session session) {
        StringBuilder response = new StringBuilder();
        response.append(Pop3Response.ok("Capability list follows"));
        response.append("\r\n");
        
        // 基本能力
        response.append("TOP").append("\r\n");
        response.append("USER").append("\r\n");
        response.append("UIDL").append("\r\n");
        response.append("RESP-CODES").append("\r\n");
        response.append("PIPELINING").append("\r\n");
        
        // 认证方式
        if (session.isInAuthorizationState()) {
            response.append("SASL PLAIN").append("\r\n");
        }
        
        // 实现信息
        response.append("IMPLEMENTATION YHM-POP3-Server-Java25").append("\r\n");
        
        response.append(Pop3Response.multiLineEnd());
        
        return response.toString();
    }
}

