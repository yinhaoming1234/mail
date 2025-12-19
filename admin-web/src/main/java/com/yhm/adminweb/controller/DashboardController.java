package com.yhm.adminweb.controller;

import com.yhm.adminweb.service.DashboardService;
import com.yhm.adminweb.service.EmailService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 仪表板控制器
 */
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EmailService emailService;

    /**
     * 仪表板首页
     */
    @GetMapping
    public String dashboard(Model model) {
        var stats = dashboardService.getDashboardStats();
        var recentEmails = emailService.findRecentEmails(10);
        
        model.addAttribute("stats", stats);
        model.addAttribute("recentEmails", recentEmails);
        
        return "dashboard/index";
    }

    /**
     * HTMX 刷新统计卡片
     */
    @GetMapping("/stats/refresh")
    @HxRequest
    public String refreshStats(Model model) {
        var stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "dashboard/index :: stats-cards";
    }

    /**
     * HTMX 刷新最近邮件
     */
    @GetMapping("/emails/recent")
    @HxRequest
    public String refreshRecentEmails(Model model) {
        var recentEmails = emailService.findRecentEmails(10);
        model.addAttribute("recentEmails", recentEmails);
        return "dashboard/index :: recent-emails";
    }
}

