package com.yhm.adminweb.repository;

import com.yhm.adminweb.entity.DeliveryLog;
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
 * 发送日志数据访问接口
 */
@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, UUID> {

    Page<DeliveryLog> findByEmailId(UUID emailId, Pageable pageable);

    Page<DeliveryLog> findByStatus(String status, Pageable pageable);

    Page<DeliveryLog> findByRecipientContainingIgnoreCase(String recipient, Pageable pageable);

    @Query("SELECT COUNT(l) FROM DeliveryLog l WHERE l.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT l.status, COUNT(l) FROM DeliveryLog l GROUP BY l.status")
    List<Object[]> getDeliveryStatusCounts();

    @Query("SELECT l.status, COUNT(l) FROM DeliveryLog l WHERE l.createdAt >= :since GROUP BY l.status")
    List<Object[]> getDeliveryStatusCountsSince(@Param("since") OffsetDateTime since);

    @Query("SELECT DATE(l.createdAt), l.status, COUNT(l) FROM DeliveryLog l WHERE l.createdAt >= :since GROUP BY DATE(l.createdAt), l.status ORDER BY DATE(l.createdAt)")
    List<Object[]> getDeliveryTrendByDate(@Param("since") OffsetDateTime since);

    List<DeliveryLog> findByEmailIdOrderByCreatedAtDesc(UUID emailId);
}

