package com.yhm.adminweb.controller;

import com.yhm.adminweb.dto.DomainForm;
import com.yhm.adminweb.entity.MailDomain;
import com.yhm.adminweb.service.MailDomainService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * 域名管理控制器
 */
@Controller
@RequestMapping("/domains")
@RequiredArgsConstructor
public class DomainController {

    private final MailDomainService domainService;

    /**
     * 域名列表页面
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailDomain> domains = domainService.search(search, pageable);
        
        model.addAttribute("domains", domains);
        model.addAttribute("search", search);
        
        return "domains/list";
    }

    /**
     * HTMX 表格刷新
     */
    @GetMapping("/table")
    @HxRequest
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailDomain> domains = domainService.search(search, pageable);
        
        model.addAttribute("domains", domains);
        model.addAttribute("search", search);
        
        return "domains/fragments/table :: domain-table";
    }

    /**
     * 新建域名表单
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("domainForm", new DomainForm());
        model.addAttribute("isEdit", false);
        return "domains/form";
    }

    /**
     * HTMX 新建表单模态框
     */
    @GetMapping("/new/modal")
    @HxRequest
    public String newFormModal(Model model) {
        model.addAttribute("domainForm", new DomainForm());
        model.addAttribute("isEdit", false);
        return "domains/fragments/form-modal :: domain-form-modal";
    }

    /**
     * 编辑域名表单
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        var domain = domainService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("域名不存在"));
        
        var form = new DomainForm(
                domain.getId(),
                domain.getDomain(),
                domain.getDescription(),
                domain.getIsLocal(),
                domain.getIsEnabled()
        );
        
        model.addAttribute("domainForm", form);
        model.addAttribute("isEdit", true);
        return "domains/form";
    }

    /**
     * HTMX 编辑表单模态框
     */
    @GetMapping("/{id}/edit/modal")
    @HxRequest
    public String editFormModal(@PathVariable UUID id, Model model) {
        var domain = domainService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("域名不存在"));
        
        var form = new DomainForm(
                domain.getId(),
                domain.getDomain(),
                domain.getDescription(),
                domain.getIsLocal(),
                domain.getIsEnabled()
        );
        
        model.addAttribute("domainForm", form);
        model.addAttribute("isEdit", true);
        return "domains/fragments/form-modal :: domain-form-modal";
    }

    /**
     * 保存域名
     */
    @PostMapping
    @HxTrigger("domain-saved")
    public String save(
            @Valid @ModelAttribute DomainForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("domainForm", form);
            model.addAttribute("isEdit", form.getId() != null);
            return "domains/fragments/form-modal :: domain-form-modal";
        }
        
        try {
            if (form.getId() != null) {
                domainService.update(form.getId(), form);
                redirectAttributes.addFlashAttribute("success", "域名更新成功");
            } else {
                domainService.create(form);
                redirectAttributes.addFlashAttribute("success", "域名创建成功");
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("domainForm", form);
            model.addAttribute("isEdit", form.getId() != null);
            return "domains/fragments/form-modal :: domain-form-modal";
        }
        
        return "redirect:/domains";
    }

    /**
     * 切换启用状态
     */
    @PostMapping("/{id}/toggle")
    @HxRequest
    @HxTrigger("domain-updated")
    public String toggle(@PathVariable UUID id, Model model) {
        var domain = domainService.toggleEnabled(id);
        long userCount = domainService.getUserCount(domain.getDomain());
        
        model.addAttribute("domain", domain);
        model.addAttribute("userCount", userCount);
        
        return "domains/fragments/table-row :: domain-row";
    }

    /**
     * 删除域名
     */
    @DeleteMapping("/{id}")
    @HxRequest
    @HxTrigger("domain-deleted")
    public String delete(@PathVariable UUID id, Model model) {
        try {
            domainService.delete(id);
            return "common/fragments/empty :: empty";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
    }
}

