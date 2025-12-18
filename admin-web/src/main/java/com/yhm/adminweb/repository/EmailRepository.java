package com.yhm.adminweb.repository;

import com.yhm.adminweb.entity.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 邮件数据访问接口
 */
@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {

    Page<Email> findByOwner(String owner, Pageable pageable);

    Page<Email> findByOwnerAndIsDeletedFalse(String owner, Pageable pageable);

    Page<Email> findBySenderContainingIgnoreCaseOrSubjectContainingIgnoreCase(
            String sender, String subject, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Email e WHERE e.isDeleted = false")
    long countActiveEmails();

    @Query("SELECT COUNT(e) FROM Email e WHERE e.isRead = false AND e.isDeleted = false")
    long countUnreadEmails();

    @Query("SELECT SUM(e.size) FROM Email e WHERE e.isDeleted = false")
    Long getTotalEmailSize();

    @Query("SELECT COUNT(e) FROM Email e WHERE e.receivedAt >= :since")
    long countEmailsSince(@Param("since") OffsetDateTime since);

    @Query("SELECT e.sender, COUNT(e) as cnt FROM Email e WHERE e.isDeleted = false GROUP BY e.sender ORDER BY cnt DESC")
    List<Object[]> getTopSenders(Pageable pageable);

    @Query("SELECT e.owner, COUNT(e) as cnt FROM Email e WHERE e.isDeleted = false GROUP BY e.owner ORDER BY cnt DESC")
    List<Object[]> getTopReceivers(Pageable pageable);

    @Query("SELECT DATE(e.receivedAt), COUNT(e) FROM Email e WHERE e.receivedAt >= :since GROUP BY DATE(e.receivedAt) ORDER BY DATE(e.receivedAt)")
    List<Object[]> getEmailCountByDate(@Param("since") OffsetDateTime since);

    @Query("SELECT e FROM Email e WHERE e.isDeleted = false ORDER BY e.receivedAt DESC")
    List<Email> findRecentEmails(Pageable pageable);
}

