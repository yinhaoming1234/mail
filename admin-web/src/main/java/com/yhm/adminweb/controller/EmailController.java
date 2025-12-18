package com.yhm.adminweb.controller;

import com.yhm.adminweb.entity.Email;
import com.yhm.adminweb.service.DeliveryLogService;
import com.yhm.adminweb.service.EmailService;
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
 * 邮件管理控制器
 */
@Controller
@RequestMapping("/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final DeliveryLogService deliveryLogService;

    /**
     * 邮件列表页面
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<Email> emails = emailService.search(search, pageable);
        
        model.addAttribute("emails", emails);
        model.addAttribute("search", search);
        
        return "emails/list";
    }

    /**
     * HTMX 表格刷新
     */
    @GetMapping("/table")
    @HxRequest
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<Email> emails = emailService.search(search, pageable);
        
        model.addAttribute("emails", emails);
        model.addAttribute("search", search);
        
        return "emails/fragments/table :: email-table";
    }

    /**
     * 邮件详情
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        var email = emailService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        var logs = deliveryLogService.findByEmailId(id);
        
        model.addAttribute("email", email);
        model.addAttribute("deliveryLogs", logs);
        
        return "emails/detail";
    }

    /**
     * HTMX 邮件详情模态框
     */
    @GetMapping("/{id}/modal")
    @HxRequest
    public String detailModal(@PathVariable UUID id, Model model) {
        var email = emailService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("邮件不存在"));
        var logs = deliveryLogService.findByEmailId(id);
        
        model.addAttribute("email", email);
        model.addAttribute("deliveryLogs", logs);
        
        return "emails/fragments/detail-modal :: email-detail-modal";
    }

    /**
     * 标记为已读
     */
    @PostMapping("/{id}/read")
    @HxRequest
    @HxTrigger("email-updated")
    public String markAsRead(@PathVariable UUID id, Model model) {
        var email = emailService.markAsRead(id);
        model.addAttribute("email", email);
        return "emails/fragments/table-row :: email-row";
    }

    /**
     * 标记为未读
     */
    @PostMapping("/{id}/unread")
    @HxRequest
    @HxTrigger("email-updated")
    public String markAsUnread(@PathVariable UUID id, Model model) {
        var email = emailService.markAsUnread(id);
        model.addAttribute("email", email);
        return "emails/fragments/table-row :: email-row";
    }

    /**
     * 软删除邮件
     */
    @PostMapping("/{id}/delete")
    @HxRequest
    @HxTrigger("email-deleted")
    public String softDelete(@PathVariable UUID id, Model model) {
        emailService.softDelete(id);
        return "common/fragments/empty :: empty";
    }

    /**
     * 恢复邮件
     */
    @PostMapping("/{id}/restore")
    @HxRequest
    @HxTrigger("email-restored")
    public String restore(@PathVariable UUID id, Model model) {
        var email = emailService.restore(id);
        model.addAttribute("email", email);
        return "emails/fragments/table-row :: email-row";
    }

    /**
     * 永久删除邮件
     */
    @DeleteMapping("/{id}")
    @HxRequest
    @HxTrigger("email-deleted")
    public String delete(@PathVariable UUID id, Model model) {
        try {
            emailService.delete(id);
            return "common/fragments/empty :: empty";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
    }
}

