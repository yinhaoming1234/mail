package com.yhm.adminweb.service;

import com.yhm.adminweb.entity.DeliveryLog;
import com.yhm.adminweb.repository.DeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 发送日志服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryLogService {

    private final DeliveryLogRepository logRepository;

    /**
     * 分页获取日志
     */
    public Page<DeliveryLog> findAll(Pageable pageable) {
        return logRepository.findAll(pageable);
    }

    /**
     * 按状态获取日志
     */
    public Page<DeliveryLog> findByStatus(String status, Pageable pageable) {
        return logRepository.findByStatus(status, pageable);
    }

    /**
     * 搜索日志
     */
    public Page<DeliveryLog> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return logRepository.findAll(pageable);
        }
        return logRepository.findByRecipientContainingIgnoreCase(keyword, pageable);
    }

    /**
     * 获取邮件的发送日志
     */
    public List<DeliveryLog> findByEmailId(UUID emailId) {
        return logRepository.findByEmailIdOrderByCreatedAtDesc(emailId);
    }

    /**
     * 根据ID获取日志
     */
    public Optional<DeliveryLog> findById(UUID id) {
        return logRepository.findById(id);
    }

    /**
     * 获取状态统计
     */
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("delivered", 0L);
        counts.put("bounced", 0L);
        counts.put("deferred", 0L);
        
        List<Object[]> data = logRepository.getDeliveryStatusCounts();
        for (Object[] row : data) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
}

