package com.yhm.adminweb.service;

import com.yhm.adminweb.dto.UserForm;
import com.yhm.adminweb.entity.User;
import com.yhm.adminweb.repository.UserRepository;
import com.yhm.adminweb.repository.MailDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final MailDomainRepository domainRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页获取用户
     */
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 搜索用户
     */
    public Page<User> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                keyword, keyword, pageable);
    }

    /**
     * 按域名获取用户
     */
    public Page<User> findByDomain(String domain, Pageable pageable) {
        return userRepository.findByDomain(domain, pageable);
    }

    /**
     * 根据ID获取用户
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * 根据邮箱获取用户
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 创建用户
     */
    @Transactional
    public User create(UserForm form) {
        // 验证域名
        if (!domainRepository.existsByDomain(form.getDomain())) {
            throw new IllegalArgumentException("域名不存在: " + form.getDomain());
        }

        String email = form.buildEmail();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }

        User user = User.builder()
                .username(form.getUsername().toLowerCase())
                .domain(form.getDomain())
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .quotaBytes(form.getQuotaBytes() != null ? form.getQuotaBytes() : 1073741824L)
                .isEnabled(form.getIsEnabled() != null ? form.getIsEnabled() : true)
                .build();

        return userRepository.save(user);
    }

    /**
     * 更新用户
     */
    @Transactional
    public User update(UUID id, UserForm form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 如果修改了用户名或域名，检查新邮箱是否存在
        String newEmail = form.getUsername().toLowerCase() + "@" + form.getDomain();
        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("邮箱已存在: " + newEmail);
        }

        user.setUsername(form.getUsername().toLowerCase());
        user.setDomain(form.getDomain());
        user.setEmail(newEmail);
        
        // 只有提供了密码才更新
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
        
        if (form.getQuotaBytes() != null) {
            user.setQuotaBytes(form.getQuotaBytes());
        }
        if (form.getIsEnabled() != null) {
            user.setIsEnabled(form.getIsEnabled());
        }

        return userRepository.save(user);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        userRepository.delete(user);
    }

    /**
     * 切换启用状态
     */
    @Transactional
    public User toggleEnabled(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setIsEnabled(!user.getIsEnabled());
        return userRepository.save(user);
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 更新配额
     */
    @Transactional
    public User updateQuota(UUID id, Long quotaBytes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setQuotaBytes(quotaBytes);
        return userRepository.save(user);
    }

    /**
     * 获取存储使用最多的用户
     */
    public List<User> findTopByStorage(int limit) {
        return userRepository.findTopUsersByStorage(Pageable.ofSize(limit));
    }
}

