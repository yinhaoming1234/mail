package com.yhm.adminweb.repository;

import com.yhm.adminweb.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByDomain(String domain);

    Page<User> findByDomain(String domain, Pageable pageable);

    List<User> findByIsEnabledTrue();

    Page<User> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
            String email, String username, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isEnabled = true")
    long countEnabledUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.domain = :domain")
    long countByDomain(@Param("domain") String domain);

    @Query("SELECT SUM(u.usedBytes) FROM User u")
    Long getTotalUsedBytes();

    @Query("SELECT SUM(u.quotaBytes) FROM User u")
    Long getTotalQuotaBytes();

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.email = :email")
    void updateLastLoginTime(@Param("email") String email, @Param("loginTime") OffsetDateTime loginTime);

    @Query("SELECT u FROM User u ORDER BY u.usedBytes DESC")
    List<User> findTopUsersByStorage(Pageable pageable);
}

