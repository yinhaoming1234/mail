package com.yhm.adminweb.service;

import com.yhm.adminweb.entity.MailQueue;
import com.yhm.adminweb.repository.MailQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 邮件队列服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MailQueueService {

    private final MailQueueRepository queueRepository;

    /**
     * 分页获取队列
     */
    public Page<MailQueue> findAll(Pageable pageable) {
        return queueRepository.findAll(pageable);
    }

    /**
     * 按状态获取队列
     */
    public Page<MailQueue> findByStatus(String status, Pageable pageable) {
        return queueRepository.findByStatus(status, pageable);
    }

    /**
     * 根据ID获取队列项
     */
    public Optional<MailQueue> findById(UUID id) {
        return queueRepository.findById(id);
    }

    /**
     * 获取队列状态统计
     */
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("pending", 0L);
        counts.put("sending", 0L);
        counts.put("sent", 0L);
        counts.put("failed", 0L);
        
        List<Object[]> data = queueRepository.getQueueStatusCounts();
        for (Object[] row : data) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    /**
     * 重试失败的邮件
     */
    @Transactional
    public MailQueue retry(UUID id) {
        MailQueue item = queueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队列项不存在"));
        
        if (!"failed".equals(item.getStatus())) {
            throw new IllegalArgumentException("只能重试失败的邮件");
        }
        
        item.setStatus("pending");
        item.setRetryCount(0);
        item.setNextRetryAt(null);
        item.setLastError(null);
        
        return queueRepository.save(item);
    }

    /**
     * 取消待发送的邮件
     */
    @Transactional
    public void cancel(UUID id) {
        MailQueue item = queueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队列项不存在"));
        
        if (!"pending".equals(item.getStatus())) {
            throw new IllegalArgumentException("只能取消待发送的邮件");
        }
        
        queueRepository.delete(item);
    }

    /**
     * 删除队列项
     */
    @Transactional
    public void delete(UUID id) {
        MailQueue item = queueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("队列项不存在"));
        queueRepository.delete(item);
    }

    /**
     * 清理旧的已发送邮件
     */
    @Transactional
    public int cleanOldSent(int daysOld) {
        OffsetDateTime before = OffsetDateTime.now().minusDays(daysOld);
        return queueRepository.deleteOldSentItems(before);
    }
}

