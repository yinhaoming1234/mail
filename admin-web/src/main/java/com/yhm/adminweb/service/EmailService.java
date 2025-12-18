package com.yhm.adminweb.service;

import com.yhm.adminweb.entity.Email;
import com.yhm.adminweb.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 邮件服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {

    private final EmailRepository emailRepository;

    /**
     * 分页获取邮件
     */
    public Page<Email> findAll(Pageable pageable) {
        return emailRepository.findAll(pageable);
    }

    /**
     * 搜索邮件
     */
    public Page<Email> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return emailRepository.findAll(pageable);
        }
        return emailRepository.findBySenderContainingIgnoreCaseOrSubjectContainingIgnoreCase(
                keyword, keyword, pageable);
    }

    /**
     * 获取用户邮件
     */
    public Page<Email> findByOwner(String owner, Pageable pageable) {
        return emailRepository.findByOwnerAndIsDeletedFalse(owner, pageable);
    }

    /**
     * 根据ID获取邮件
     */
    public Optional<Email> findById(UUID id) {
        return emailRepository.findById(id);
    }

    /**
     * 标记为已读
     */
    @Transactional
    public Email markAsRead(UUID id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        email.setIsRead(true);
        return emailRepository.save(email);
    }

    /**
     * 标记为未读
     */
    @Transactional
    public Email markAsUnread(UUID id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        email.setIsRead(false);
        return emailRepository.save(email);
    }

    /**
     * 软删除邮件
     */
    @Transactional
    public Email softDelete(UUID id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        email.setIsDeleted(true);
        return emailRepository.save(email);
    }

    /**
     * 恢复邮件
     */
    @Transactional
    public Email restore(UUID id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        email.setIsDeleted(false);
        return emailRepository.save(email);
    }

    /**
     * 永久删除邮件
     */
    @Transactional
    public void delete(UUID id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        emailRepository.delete(email);
    }

    /**
     * 获取最近邮件
     */
    public List<Email> findRecentEmails(int limit) {
        return emailRepository.findRecentEmails(Pageable.ofSize(limit));
    }
}

