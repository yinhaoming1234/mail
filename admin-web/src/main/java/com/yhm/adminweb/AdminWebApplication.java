package com.yhm.adminweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 邮件管理后台应用主类
 * 
 * 基于 Spring Boot 4.0 + Java 25 构建
 * 前后端不分离架构，使用 Thymeleaf + HTMX
 * 
 * 主要功能：
 * - 域名管理
 * - 用户管理  
 * - 邮件管理
 * - 发送队列监控
 * - 投递日志查看
 * - 仪表板统计
 * 
 * 亮点特性：
 * - 现代化赛博朋克风格 UI
 * - HTMX 无刷新交互体验
 * - 实时数据刷新
 * - 深色/浅色主题切换
 * - 响应式设计
 * - Spring Boot 4.0 虚拟线程支持
 */
@SpringBootApplication
public class AdminWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminWebApplication.class, args);
    }

}
