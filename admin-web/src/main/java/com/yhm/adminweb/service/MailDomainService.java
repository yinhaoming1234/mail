package com.yhm.adminweb.service;

import com.yhm.adminweb.dto.DomainForm;
import com.yhm.adminweb.entity.MailDomain;
import com.yhm.adminweb.repository.MailDomainRepository;
import com.yhm.adminweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 邮件域名服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MailDomainService {

    private final MailDomainRepository domainRepository;
    private final UserRepository userRepository;

    /**
     * 获取所有域名
     */
    public List<MailDomain> findAll() {
        return domainRepository.findAll();
    }

    /**
     * 分页获取域名
     */
    public Page<MailDomain> findAll(Pageable pageable) {
        return domainRepository.findAll(pageable);
    }

    /**
     * 搜索域名
     */
    public Page<MailDomain> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return domainRepository.findAll(pageable);
        }
        return domainRepository.findByDomainContainingIgnoreCase(keyword, pageable);
    }

    /**
     * 获取启用的域名
     */
    public List<MailDomain> findEnabled() {
        return domainRepository.findByIsEnabledTrue();
    }

    /**
     * 根据ID获取域名
     */
    public Optional<MailDomain> findById(UUID id) {
        return domainRepository.findById(id);
    }

    /**
     * 根据域名查找
     */
    public Optional<MailDomain> findByDomain(String domain) {
        return domainRepository.findByDomain(domain);
    }

    /**
     * 检查域名是否存在
     */
    public boolean existsByDomain(String domain) {
        return domainRepository.existsByDomain(domain);
    }

    /**
     * 创建域名
     */
    @Transactional
    public MailDomain create(DomainForm form) {
        if (domainRepository.existsByDomain(form.getDomain())) {
            throw new IllegalArgumentException("域名已存在: " + form.getDomain());
        }

        MailDomain domain = MailDomain.builder()
                .domain(form.getDomain().toLowerCase())
                .description(form.getDescription())
                .isLocal(form.getIsLocal() != null ? form.getIsLocal() : true)
                .isEnabled(form.getIsEnabled() != null ? form.getIsEnabled() : true)
                .build();

        return domainRepository.save(domain);
    }

    /**
     * 更新域名
     */
    @Transactional
    public MailDomain update(UUID id, DomainForm form) {
        MailDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("域名不存在"));

        // 检查是否修改了域名且新域名已存在
        if (!domain.getDomain().equals(form.getDomain()) && 
            domainRepository.existsByDomain(form.getDomain())) {
            throw new IllegalArgumentException("域名已存在: " + form.getDomain());
        }

        domain.setDomain(form.getDomain().toLowerCase());
        domain.setDescription(form.getDescription());
        if (form.getIsLocal() != null) domain.setIsLocal(form.getIsLocal());
        if (form.getIsEnabled() != null) domain.setIsEnabled(form.getIsEnabled());

        return domainRepository.save(domain);
    }

    /**
     * 删除域名
     */
    @Transactional
    public void delete(UUID id) {
        MailDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("域名不存在"));

        // 检查是否有关联用户
        long userCount = userRepository.countByDomain(domain.getDomain());
        if (userCount > 0) {
            throw new IllegalArgumentException("该域名下还有 " + userCount + " 个用户，无法删除");
        }

        domainRepository.delete(domain);
    }

    /**
     * 切换启用状态
     */
    @Transactional
    public MailDomain toggleEnabled(UUID id) {
        MailDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("域名不存在"));
        domain.setIsEnabled(!domain.getIsEnabled());
        return domainRepository.save(domain);
    }

    /**
     * 获取域名下的用户数
     */
    public long getUserCount(String domain) {
        return userRepository.countByDomain(domain);
    }
}

