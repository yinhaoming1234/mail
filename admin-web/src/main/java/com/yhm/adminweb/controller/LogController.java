package com.yhm.adminweb.controller;

import com.yhm.adminweb.entity.DeliveryLog;
import com.yhm.adminweb.service.DeliveryLogService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 发送日志控制器
 */
@Controller
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final DeliveryLogService logService;

    /**
     * 日志列表页面
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DeliveryLog> logs;
        
        if (status != null && !status.isBlank()) {
            logs = logService.findByStatus(status, pageable);
        } else if (search != null && !search.isBlank()) {
            logs = logService.search(search, pageable);
        } else {
            logs = logService.findAll(pageable);
        }
        
        var statusCounts = logService.getStatusCounts();
        
        model.addAttribute("logs", logs);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search", search);
        model.addAttribute("statusCounts", statusCounts);
        
        return "logs/list";
    }

    /**
     * HTMX 表格刷新
     */
    @GetMapping("/table")
    @HxRequest
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DeliveryLog> logs;
        
        if (status != null && !status.isBlank()) {
            logs = logService.findByStatus(status, pageable);
        } else if (search != null && !search.isBlank()) {
            logs = logService.search(search, pageable);
        } else {
            logs = logService.findAll(pageable);
        }
        
        model.addAttribute("logs", logs);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search", search);
        
        return "logs/fragments/table :: log-table";
    }

    /**
     * 日志详情
     */
    @GetMapping("/{id}")
    @HxRequest
    public String detail(@PathVariable UUID id, Model model) {
        var log = logService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("日志不存在"));
        
        model.addAttribute("log", log);
        return "logs/fragments/detail-modal :: log-detail-modal";
    }
}

