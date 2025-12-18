package com.yhm.smtp.protocol;

import com.yhm.mail.core.model.Email;
import com.yhm.mail.core.util.EmailParser;
import com.yhm.smtp.config.SmtpServerConfig;
import com.yhm.smtp.db.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMTP 命令处理器
 * 处理各种 SMTP 命令并返回响应
 */
public class SmtpCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SmtpCommandHandler.class);

    // 正则表达式模式
    private static final Pattern MAIL_FROM_PATTERN = Pattern.compile(
            "MAIL\\s+FROM\\s*:\\s*<([^>]*)>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RCPT_TO_PATTERN = Pattern.compile(
            "RCPT\\s+TO\\s*:\\s*<([^>]*)>",
            Pattern.CASE_INSENSITIVE);

    private final SmtpServerConfig config;
    private final EmailRepository emailRepository;

    public SmtpCommandHandler(SmtpServerConfig config) {
        this.config = config;
        this.emailRepository = new EmailRepository();
    }

    /**
     * 处理 SMTP 命令
     *
     * @param line    原始命令行
     * @param session 当前会话
     * @return 响应字符串
     */
    public String handleCommand(String line, SmtpSession session) {
        // 如果在 DATA 状态，处理邮件数据（包括空行）
        if (session.getState() == SmtpSession.State.DATA) {
            // 在 DATA 模式下，空行是邮件内容的一部分，不应被视为错误
            return handleDataContent(line == null ? "" : line, session);
        }

        // 在非 DATA 状态下，空行是语法错误
        if (line == null || line.isBlank()) {
            return SmtpResponse.syntaxError();
        }

        SmtpCommand command = SmtpCommand.parse(line);

        return switch (command) {
            case HELO -> handleHelo(line, session);
            case EHLO -> handleEhlo(line, session);
            case MAIL -> handleMailFrom(line, session);
            case RCPT -> handleRcptTo(line, session);
            case DATA -> handleData(session);
            case RSET -> handleRset(session);
            case NOOP -> handleNoop();
            case QUIT -> handleQuit(session);
            case VRFY -> handleVrfy(line, session);
            case HELP -> handleHelp();
            case EXPN -> SmtpResponse.commandNotImplemented();
            case AUTH -> SmtpResponse.commandNotImplemented();
            case STARTTLS -> SmtpResponse.commandNotImplemented();
            case UNKNOWN -> SmtpResponse.syntaxError();
        };
    }

    /**
     * 处理 HELO 命令
     */
    private String handleHelo(String line, SmtpSession session) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return SmtpResponse.parameterSyntaxError();
        }

        session.setClientHostname(parts[1].trim());
        session.setState(SmtpSession.State.READY);
        session.setExtendedMode(false);

        log.info("HELO from {} ({})", parts[1].trim(), session.getRemoteAddress());

        return SmtpResponse.ok(config.getDomain() + " Hello " + parts[1].trim());
    }

    /**
     * 处理 EHLO 命令
     */
    private String handleEhlo(String line, SmtpSession session) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return SmtpResponse.parameterSyntaxError();
        }

        session.setClientHostname(parts[1].trim());
        session.setState(SmtpSession.State.READY);
        session.setExtendedMode(true);

        log.info("EHLO from {} ({})", parts[1].trim(), session.getRemoteAddress());

        // 返回支持的扩展
        return SmtpResponse.ehloResponse(
                config.getDomain(),
                "SIZE " + config.getMaxMessageSize(),
                "8BITMIME",
                "PIPELINING",
                "ENHANCEDSTATUSCODES",
                "HELP");
    }

    /**
     * 处理 MAIL FROM 命令
     */
    private String handleMailFrom(String line, SmtpSession session) {
        // 检查命令序列
        if (session.getState() != SmtpSession.State.READY) {
            return SmtpResponse.badSequence();
        }

        Matcher matcher = MAIL_FROM_PATTERN.matcher(line);
        if (!matcher.find()) {
            return SmtpResponse.parameterSyntaxError();
        }

        String sender = matcher.group(1);

        // 允许空发件人（用于退信）
        if (!sender.isEmpty() && !EmailParser.isValidEmail(sender)) {
            return SmtpResponse.parameterSyntaxError();
        }

        session.setSender(sender);
        session.setState(SmtpSession.State.MAIL_FROM_SET);

        log.debug("MAIL FROM: <{}>", sender);

        return SmtpResponse.ok("Sender <" + sender + "> OK");
    }

    /**
     * 处理 RCPT TO 命令
     */
    private String handleRcptTo(String line, SmtpSession session) {
        // 检查命令序列
        if (session.getState() != SmtpSession.State.MAIL_FROM_SET &&
                session.getState() != SmtpSession.State.RCPT_TO_SET) {
            return SmtpResponse.badSequence();
        }

        Matcher matcher = RCPT_TO_PATTERN.matcher(line);
        if (!matcher.find()) {
            return SmtpResponse.parameterSyntaxError();
        }

        String recipient = matcher.group(1);

        if (recipient.isEmpty() || !EmailParser.isValidEmail(recipient)) {
            return SmtpResponse.parameterSyntaxError();
        }

        // 检查收件人数量限制
        if (session.getRecipients().size() >= config.getMaxRecipients()) {
            return SmtpResponse.transactionFailed("Too many recipients");
        }

        // 检查是否为本地域名
        String domain = EmailParser.extractDomain(recipient);
        try {
            if (!emailRepository.isLocalDomain(domain)) {
                // 不接受非本地域名的邮件（不做中继）
                log.warn("拒绝非本地域名邮件: {} -> {}", session.getSender(), recipient);
                return SmtpResponse.userNotLocal(recipient);
            }

            // 检查用户是否存在
            if (!emailRepository.userExists(recipient)) {
                log.warn("用户不存在: {}", recipient);
                return SmtpResponse.userNotFound(recipient);
            }
        } catch (SQLException e) {
            log.error("数据库错误: {}", e.getMessage());
            return SmtpResponse.localError();
        }

        session.addRecipient(recipient);
        session.setState(SmtpSession.State.RCPT_TO_SET);

        log.debug("RCPT TO: <{}>", recipient);

        return SmtpResponse.ok("Recipient <" + recipient + "> OK");
    }

    /**
     * 处理 DATA 命令
     */
    private String handleData(SmtpSession session) {
        // 检查命令序列
        if (session.getState() != SmtpSession.State.RCPT_TO_SET) {
            return SmtpResponse.badSequence();
        }

        if (session.getRecipients().isEmpty()) {
            return SmtpResponse.badSequence();
        }

        session.setState(SmtpSession.State.DATA);
        session.clearMailData();

        log.debug("开始接收邮件数据");

        return SmtpResponse.startMailInput();
    }

    /**
     * 处理邮件数据内容
     */
    private String handleDataContent(String line, SmtpSession session) {
        // 检查是否是结束标记
        if (".".equals(line)) {
            return processMailData(session);
        }

        // 透明处理：以点开头的行，移除第一个点
        String content = line;
        if (line.startsWith("..")) {
            content = line.substring(1);
        }

        // 检查邮件大小
        if (session.getMailData().length() + content.length() > config.getMaxMessageSize()) {
            session.resetTransaction();
            return SmtpResponse.storageExceeded();
        }

        session.appendMailData(content);

        // 返回 null 表示继续接收数据
        return null;
    }

    /**
     * 处理接收完成的邮件数据
     */
    private String processMailData(SmtpSession session) {
        String rawContent = session.getMailDataString();

        try {
            // 解析邮件
            Email email = EmailParser.parse(rawContent, session.getSender(), session.getRecipients());

            // 为每个收件人保存邮件
            emailRepository.saveForRecipients(email);

            log.info("邮件已保存: from={}, to={}, subject={}",
                    email.getSender(),
                    email.getRecipients(),
                    email.getSubject());

            // 重置事务状态
            session.resetTransaction();

            return SmtpResponse.ok("Message accepted for delivery");

        } catch (SQLException e) {
            log.error("保存邮件失败: {}", e.getMessage(), e);
            session.resetTransaction();
            return SmtpResponse.localError();
        }
    }

    /**
     * 处理 RSET 命令
     */
    private String handleRset(SmtpSession session) {
        session.resetTransaction();
        log.debug("会话已重置");
        return SmtpResponse.ok("Reset OK");
    }

    /**
     * 处理 NOOP 命令
     */
    private String handleNoop() {
        return SmtpResponse.ok();
    }

    /**
     * 处理 QUIT 命令
     */
    private String handleQuit(SmtpSession session) {
        session.setState(SmtpSession.State.QUIT);
        log.debug("客户端断开连接: {}", session.getRemoteAddress());
        return SmtpResponse.serviceClosing(config.getDomain());
    }

    /**
     * 处理 VRFY 命令
     */
    private String handleVrfy(String line, SmtpSession session) {
        // 出于安全考虑，禁用 VRFY 命令
        return SmtpResponse.commandNotImplemented();
    }

    /**
     * 处理 HELP 命令
     */
    private String handleHelp() {
        return "214-Commands supported:\r\n" +
                "214-  HELO EHLO MAIL RCPT DATA\r\n" +
                "214-  RSET NOOP QUIT HELP\r\n" +
                "214 End of HELP info";
    }
}
