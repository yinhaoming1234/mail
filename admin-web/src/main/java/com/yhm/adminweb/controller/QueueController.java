package com.yhm.adminweb.controller;

import com.yhm.adminweb.entity.MailQueue;
import com.yhm.adminweb.service.MailQueueService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 邮件队列控制器
 */
@Controller
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final MailQueueService queueService;

    /**
     * 队列列表页面
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailQueue> queue;
        
        if (status != null && !status.isBlank()) {
            queue = queueService.findByStatus(status, pageable);
        } else {
            queue = queueService.findAll(pageable);
        }
        
        var statusCounts = queueService.getStatusCounts();
        
        model.addAttribute("queue", queue);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statusCounts", statusCounts);
        
        return "queue/list";
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
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailQueue> queue;
        
        if (status != null && !status.isBlank()) {
            queue = queueService.findByStatus(status, pageable);
        } else {
            queue = queueService.findAll(pageable);
        }
        
        model.addAttribute("queue", queue);
        model.addAttribute("selectedStatus", status);
        
        return "queue/fragments/table :: queue-table";
    }

    /**
     * HTMX 状态统计刷新
     */
    @GetMapping("/stats")
    @HxRequest
    public String stats(Model model) {
        var statusCounts = queueService.getStatusCounts();
        model.addAttribute("statusCounts", statusCounts);
        return "queue/fragments/status-tabs :: status-tabs";
    }

    /**
     * 重试发送
     */
    @PostMapping("/{id}/retry")
    @HxRequest
    @HxTrigger("queue-updated")
    public String retry(@PathVariable UUID id, Model model) {
        try {
            var item = queueService.retry(id);
            model.addAttribute("item", item);
            return "queue/fragments/table-row :: queue-row";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
    }

    /**
     * 取消发送
     */
    @PostMapping("/{id}/cancel")
    @HxRequest
    @HxTrigger("queue-deleted")
    public String cancel(@PathVariable UUID id, Model model) {
        try {
            queueService.cancel(id);
            return "common/fragments/empty :: empty";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
    }

    /**
     * 删除队列项
     */
    @DeleteMapping("/{id}")
    @HxRequest
    @HxTrigger("queue-deleted")
    public String delete(@PathVariable UUID id, Model model) {
        try {
            queueService.delete(id);
            return "common/fragments/empty :: empty";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
    }
}

