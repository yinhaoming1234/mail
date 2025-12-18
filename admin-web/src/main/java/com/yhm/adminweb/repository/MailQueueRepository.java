package com.yhm.adminweb.repository;

import com.yhm.adminweb.entity.MailQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 邮件队列数据访问接口
 */
@Repository
public interface MailQueueRepository extends JpaRepository<MailQueue, UUID> {

    Page<MailQueue> findByStatus(String status, Pageable pageable);

    List<MailQueue> findByStatusAndNextRetryAtBefore(String status, OffsetDateTime time);

    @Query("SELECT COUNT(q) FROM MailQueue q WHERE q.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT q.status, COUNT(q) FROM MailQueue q GROUP BY q.status")
    List<Object[]> getQueueStatusCounts();

    @Modifying
    @Query("UPDATE MailQueue q SET q.status = :newStatus WHERE q.status = :oldStatus AND q.nextRetryAt <= :now")
    int updateStatusForRetry(@Param("oldStatus") String oldStatus, 
                             @Param("newStatus") String newStatus, 
                             @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM MailQueue q WHERE q.status = 'sent' AND q.updatedAt < :before")
    int deleteOldSentItems(@Param("before") OffsetDateTime before);
}

