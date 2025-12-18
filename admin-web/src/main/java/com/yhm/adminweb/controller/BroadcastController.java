package com.yhm.adminweb.controller;

import com.yhm.adminweb.dto.BroadcastEmailForm;
import com.yhm.adminweb.service.BroadcastEmailService;
import com.yhm.adminweb.service.MailDomainService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 群发邮件控制器
 */
@Controller
@RequestMapping("/broadcast")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastEmailService broadcastService;
    private final MailDomainService domainService;

    /**
     * 群发邮件页面
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("broadcastForm", new BroadcastEmailForm());
        model.addAttribute("domains", domainService.findEnabled());
        return "broadcast/index";
    }

    /**
     * HTMX 群发表单模态框
     */
    @GetMapping("/modal")
    @HxRequest
    public String modal(Model model) {
        model.addAttribute("broadcastForm", new BroadcastEmailForm());
        model.addAttribute("domains", domainService.findEnabled());
        return "broadcast/fragments/form-modal :: broadcast-form-modal";
    }

    /**
     * 预览收件人数量
     */
    @PostMapping("/preview")
    @HxRequest
    public String preview(@ModelAttribute BroadcastEmailForm form, Model model) {
        int count = broadcastService.getRecipientCount(form);
        model.addAttribute("recipientCount", count);
        return "broadcast/fragments/preview :: recipient-preview";
    }

    /**
     * 发送群发邮件
     */
    @PostMapping
    @HxTrigger("broadcast-sent")
    public String send(
            @Valid @ModelAttribute BroadcastEmailForm form,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("broadcastForm", form);
            model.addAttribute("domains", domainService.findEnabled());
            return "broadcast/fragments/form-modal :: broadcast-form-modal";
        }

        try {
            String senderEmail = "system@localhost"; // 系统发送者
            int sentCount = broadcastService.sendBroadcast(form, senderEmail);
            
            if (sentCount > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("群发邮件成功！已发送给 %d 位用户", sentCount));
            } else {
                redirectAttributes.addFlashAttribute("warning", "没有找到符合条件的收件人");
            }
        } catch (Exception e) {
            model.addAttribute("error", "发送失败: " + e.getMessage());
            model.addAttribute("broadcastForm", form);
            model.addAttribute("domains", domainService.findEnabled());
            return "broadcast/fragments/form-modal :: broadcast-form-modal";
        }

        return "redirect:/broadcast";
    }

    /**
     * HTMX 发送群发邮件
     */
    @PostMapping("/send")
    @HxRequest
    @HxTrigger("broadcast-sent")
    public String sendHtmx(
            @Valid @ModelAttribute BroadcastEmailForm form,
            BindingResult result,
            Authentication authentication,
            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("broadcastForm", form);
            model.addAttribute("domains", domainService.findEnabled());
            model.addAttribute("error", "请检查表单填写是否正确");
            return "broadcast/fragments/form-modal :: broadcast-form-modal";
        }

        try {
            String senderEmail = "system@localhost";
            int sentCount = broadcastService.sendBroadcast(form, senderEmail);
            
            if (sentCount > 0) {
                model.addAttribute("success", String.format("群发邮件成功！已发送给 %d 位用户", sentCount));
                return "common/fragments/success :: success-toast";
            } else {
                model.addAttribute("warning", "没有找到符合条件的收件人");
                return "common/fragments/warning :: warning-toast";
            }
        } catch (Exception e) {
            model.addAttribute("error", "发送失败: " + e.getMessage());
            model.addAttribute("broadcastForm", form);
            model.addAttribute("domains", domainService.findEnabled());
            return "broadcast/fragments/form-modal :: broadcast-form-modal";
        }
    }
}
