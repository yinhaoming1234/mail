package com.yhm.adminweb.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表板统计数据 DTO
 */
@Data
@Builder
public class DashboardStats {
    
    // 概览数据
    private long totalDomains;
    private long enabledDomains;
    private long totalUsers;
    private long enabledUsers;
    private long totalEmails;
    private long unreadEmails;
    
    // 存储统计
    private Long totalUsedBytes;
    private Long totalQuotaBytes;
    private String formattedUsedStorage;
    private String formattedTotalStorage;
    private double storageUsagePercent;
    
    // 队列统计
    private long pendingQueue;
    private long sentQueue;
    private long failedQueue;
    
    // 投递统计
    private long deliveredCount;
    private long bouncedCount;
    private long deferredCount;
    
    // 今日统计
    private long todayEmails;
    private long todayDelivered;
    private long todayBounced;
    
    // 趋势数据
    private List<Map<String, Object>> emailTrend;
    private List<Map<String, Object>> deliveryTrend;
    
    // 排行榜数据
    private List<Map<String, Object>> topSenders;
    private List<Map<String, Object>> topReceivers;
    private List<Map<String, Object>> topStorageUsers;
}

