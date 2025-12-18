package com.yhm.adminweb.service;

import com.yhm.adminweb.dto.BroadcastEmailForm;
import com.yhm.adminweb.entity.Email;
import com.yhm.adminweb.entity.User;
import com.yhm.adminweb.repository.EmailRepository;
import com.yhm.adminweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 群发邮件服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastEmailService {

    private final UserRepository userRepository;
    private final EmailRepository emailRepository;

    /**
     * 群发邮件
     * @param form 群发邮件表单
     * @param senderEmail 发送者邮箱
     * @return 成功发送的邮件数量
     */
    @Transactional
    public int sendBroadcast(BroadcastEmailForm form, String senderEmail) {
        List<String> targetEmails = getTargetEmails(form);
        
        if (targetEmails.isEmpty()) {
            log.warn("No recipients found for broadcast email");
            return 0;
        }

        int successCount = 0;
        String rawContent = buildRawContent(senderEmail, form.getSubject(), form.getBody());

        for (String recipientEmail : targetEmails) {
            try {
                Email email = Email.builder()
                        .sender(senderEmail)
                        .recipients(new String[]{recipientEmail})
                        .subject(form.getSubject())
                        .body(form.getBody())
                        .rawContent(rawContent)
                        .size((long) rawContent.length())
                        .receivedAt(OffsetDateTime.now())
                        .isRead(false)
                        .isDeleted(false)
                        .owner(recipientEmail)
                        .build();
                
                emailRepository.save(email);
                successCount++;
                log.debug("Broadcast email sent to: {}", recipientEmail);
            } catch (Exception e) {
                log.error("Failed to send broadcast email to: {}", recipientEmail, e);
            }
        }

        log.info("Broadcast email sent successfully to {} recipients", successCount);
        return successCount;
    }

    /**
     * 获取目标收件人列表
     */
    private List<String> getTargetEmails(BroadcastEmailForm form) {
        // 如果指定了收件人列表，直接使用
        if (form.getRecipients() != null && !form.getRecipients().isEmpty()) {
            return form.getRecipients();
        }

        List<User> users;
        
        // 只发送给管理员
        if (Boolean.TRUE.equals(form.getAdminOnly())) {
            users = userRepository.findByIsAdminTrue();
        }
        // 按域名筛选
        else if (form.getTargetDomain() != null && !form.getTargetDomain().isBlank()) {
            users = userRepository.findByDomain(form.getTargetDomain());
        }
        // 发送给所有启用的用户
        else {
            users = userRepository.findByIsEnabledTrue();
        }

        return users.stream()
                .filter(User::getIsEnabled)
                .map(User::getEmail)
                .toList();
    }

    /**
     * 构建原始邮件内容
     */
    private String buildRawContent(String from, String subject, String body) {
        return String.format("""
                From: <%s>
                Subject: %s
                Date: %s
                MIME-Version: 1.0
                Content-Type: text/plain; charset=utf-8
                
                %s
                """, from, subject, OffsetDateTime.now(), body);
    }

    /**
     * 获取可用的收件人数量预览
     */
    public int getRecipientCount(BroadcastEmailForm form) {
        return getTargetEmails(form).size();
    }

    /**
     * 获取所有可用域名
     */
    public List<String> getAvailableDomains() {
        return userRepository.findByIsEnabledTrue()
                .stream()
                .map(User::getDomain)
                .distinct()
                .toList();
    }
}
