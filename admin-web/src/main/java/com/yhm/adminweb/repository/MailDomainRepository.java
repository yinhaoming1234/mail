package com.yhm.adminweb.repository;

import com.yhm.adminweb.entity.MailDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 邮件域名数据访问接口
 */
@Repository
public interface MailDomainRepository extends JpaRepository<MailDomain, UUID> {

    Optional<MailDomain> findByDomain(String domain);

    boolean existsByDomain(String domain);

    List<MailDomain> findByIsEnabledTrue();

    List<MailDomain> findByIsLocalTrue();

    Page<MailDomain> findByDomainContainingIgnoreCase(String domain, Pageable pageable);

    @Query("SELECT COUNT(d) FROM MailDomain d WHERE d.isEnabled = true")
    long countEnabledDomains();

    @Query("SELECT COUNT(d) FROM MailDomain d WHERE d.isLocal = true")
    long countLocalDomains();
}

