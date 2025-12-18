package com.yhm.adminweb.service;

import com.yhm.adminweb.dto.DashboardStats;
import com.yhm.adminweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * 仪表板服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MailDomainRepository domainRepository;
    private final UserRepository userRepository;
    private final EmailRepository emailRepository;
    private final MailQueueRepository queueRepository;
    private final DeliveryLogRepository deliveryLogRepository;

    /**
     * 获取仪表板统计数据
     */
    public DashboardStats getDashboardStats() {
        OffsetDateTime today = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime last7Days = today.minusDays(7);

        // 基础统计
        long totalDomains = domainRepository.count();
        long enabledDomains = domainRepository.countEnabledDomains();
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.countEnabledUsers();
        long totalEmails = emailRepository.countActiveEmails();
        long unreadEmails = emailRepository.countUnreadEmails();

        // 存储统计
        Long totalUsedBytes = Optional.ofNullable(userRepository.getTotalUsedBytes()).orElse(0L);
        Long totalQuotaBytes = Optional.ofNullable(userRepository.getTotalQuotaBytes()).orElse(0L);

        // 队列统计
        long pendingQueue = queueRepository.countByStatus("pending");
        long sentQueue = queueRepository.countByStatus("sent");
        long failedQueue = queueRepository.countByStatus("failed");

        // 投递统计
        long deliveredCount = deliveryLogRepository.countByStatus("delivered");
        long bouncedCount = deliveryLogRepository.countByStatus("bounced");
        long deferredCount = deliveryLogRepository.countByStatus("deferred");

        // 今日统计
        long todayEmails = emailRepository.countEmailsSince(today);
        Map<String, Long> todayDeliveryStats = getTodayDeliveryStats(today);

        // 趋势数据
        List<Map<String, Object>> emailTrend = getEmailTrend(last7Days);
        List<Map<String, Object>> deliveryTrend = getDeliveryTrend(last7Days);

        // 排行榜
        List<Map<String, Object>> topSenders = getTopSenders();
        List<Map<String, Object>> topReceivers = getTopReceivers();
        List<Map<String, Object>> topStorageUsers = getTopStorageUsers();

        return DashboardStats.builder()
                .totalDomains(totalDomains)
                .enabledDomains(enabledDomains)
                .totalUsers(totalUsers)
                .enabledUsers(enabledUsers)
                .totalEmails(totalEmails)
                .unreadEmails(unreadEmails)
                .totalUsedBytes(totalUsedBytes)
                .totalQuotaBytes(totalQuotaBytes)
                .formattedUsedStorage(formatBytes(totalUsedBytes))
                .formattedTotalStorage(formatBytes(totalQuotaBytes))
                .storageUsagePercent(totalQuotaBytes > 0 ? (double) totalUsedBytes / totalQuotaBytes * 100 : 0)
                .pendingQueue(pendingQueue)
                .sentQueue(sentQueue)
                .failedQueue(failedQueue)
                .deliveredCount(deliveredCount)
                .bouncedCount(bouncedCount)
                .deferredCount(deferredCount)
                .todayEmails(todayEmails)
                .todayDelivered(todayDeliveryStats.getOrDefault("delivered", 0L))
                .todayBounced(todayDeliveryStats.getOrDefault("bounced", 0L))
                .emailTrend(emailTrend)
                .deliveryTrend(deliveryTrend)
                .topSenders(topSenders)
                .topReceivers(topReceivers)
                .topStorageUsers(topStorageUsers)
                .build();
    }

    private Map<String, Long> getTodayDeliveryStats(OffsetDateTime today) {
        Map<String, Long> stats = new HashMap<>();
        List<Object[]> counts = deliveryLogRepository.getDeliveryStatusCountsSince(today);
        for (Object[] row : counts) {
            stats.put((String) row[0], (Long) row[1]);
        }
        return stats;
    }

    private List<Map<String, Object>> getEmailTrend(OffsetDateTime since) {
        List<Object[]> data = emailRepository.getEmailCountByDate(since);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0] != null ? row[0].toString() : "");
            item.put("count", row[1]);
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> getDeliveryTrend(OffsetDateTime since) {
        List<Object[]> data = deliveryLogRepository.getDeliveryTrendByDate(since);
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
        
        for (Object[] row : data) {
            String date = row[0] != null ? row[0].toString() : "";
            String status = (String) row[1];
            Long count = (Long) row[2];
            
            grouped.computeIfAbsent(date, k -> new HashMap<>()).put(status, count);
        }
        
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry : grouped.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey());
            item.putAll(entry.getValue());
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> getTopSenders() {
        List<Object[]> data = emailRepository.getTopSenders(PageRequest.of(0, 5));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("sender", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> getTopReceivers() {
        List<Object[]> data = emailRepository.getTopReceivers(PageRequest.of(0, 5));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("receiver", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> getTopStorageUsers() {
        var users = userRepository.findTopUsersByStorage(PageRequest.of(0, 5));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var user : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("email", user.getEmail());
            item.put("used", user.getFormattedUsed());
            item.put("quota", user.getFormattedQuota());
            item.put("percent", String.format("%.1f", user.getUsedPercentage()));
            result.add(item);
        }
        return result;
    }

    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

