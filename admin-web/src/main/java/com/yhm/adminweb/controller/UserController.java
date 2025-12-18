package com.yhm.adminweb.controller;

import com.yhm.adminweb.dto.UserForm;
import com.yhm.adminweb.entity.User;
import com.yhm.adminweb.service.MailDomainService;
import com.yhm.adminweb.service.UserService;
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
 * 用户管理控制器
 */
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MailDomainService domainService;

    /**
     * 用户列表页面
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String domain,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users;
        
        if (domain != null && !domain.isBlank()) {
            users = userService.findByDomain(domain, pageable);
        } else {
            users = userService.search(search, pageable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("selectedDomain", domain);
        model.addAttribute("domains", domainService.findEnabled());
        
        return "users/list";
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
            @RequestParam(required = false) String domain,
            Model model) {
        
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users;
        
        if (domain != null && !domain.isBlank()) {
            users = userService.findByDomain(domain, pageable);
        } else {
            users = userService.search(search, pageable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("selectedDomain", domain);
        
        return "users/fragments/table :: user-table";
    }

    /**
     * 用户详情
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        var user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        model.addAttribute("user", user);
        return "users/detail";
    }

    /**
     * 新建用户表单
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("domains", domainService.findEnabled());
        model.addAttribute("isEdit", false);
        return "users/form";
    }

    /**
     * HTMX 新建表单模态框
     */
    @GetMapping("/new/modal")
    @HxRequest
    public String newFormModal(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("domains", domainService.findEnabled());
        model.addAttribute("isEdit", false);
        return "users/fragments/form-modal :: user-form-modal";
    }

    /**
     * 编辑用户表单
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        var user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        var form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setDomain(user.getDomain());
        form.setQuotaBytes(user.getQuotaBytes());
        form.setIsEnabled(user.getIsEnabled());
        
        model.addAttribute("userForm", form);
        model.addAttribute("domains", domainService.findEnabled());
        model.addAttribute("isEdit", true);
        return "users/form";
    }

    /**
     * HTMX 编辑表单模态框
     */
    @GetMapping("/{id}/edit/modal")
    @HxRequest
    public String editFormModal(@PathVariable UUID id, Model model) {
        var user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        var form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setDomain(user.getDomain());
        form.setQuotaBytes(user.getQuotaBytes());
        form.setIsEnabled(user.getIsEnabled());
        
        model.addAttribute("userForm", form);
        model.addAttribute("domains", domainService.findEnabled());
        model.addAttribute("isEdit", true);
        return "users/fragments/form-modal :: user-form-modal";
    }

    /**
     * 保存用户
     */
    @PostMapping
    @HxTrigger("user-saved")
    public String save(
            @Valid @ModelAttribute UserForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // 新建用户时密码必填
        if (form.getId() == null && (form.getPassword() == null || form.getPassword().isBlank())) {
            result.rejectValue("password", "NotBlank", "密码不能为空");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("userForm", form);
            model.addAttribute("domains", domainService.findEnabled());
            model.addAttribute("isEdit", form.getId() != null);
            return "users/fragments/form-modal :: user-form-modal";
        }
        
        try {
            if (form.getId() != null) {
                userService.update(form.getId(), form);
                redirectAttributes.addFlashAttribute("success", "用户更新成功");
            } else {
                userService.create(form);
                redirectAttributes.addFlashAttribute("success", "用户创建成功");
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userForm", form);
            model.addAttribute("domains", domainService.findEnabled());
            model.addAttribute("isEdit", form.getId() != null);
            return "users/fragments/form-modal :: user-form-modal";
        }
        
        return "redirect:/users";
    }

    /**
     * 切换启用状态
     */
    @PostMapping("/{id}/toggle")
    @HxRequest
    @HxTrigger("user-updated")
    public String toggle(@PathVariable UUID id, Model model) {
        var user = userService.toggleEnabled(id);
        model.addAttribute("user", user);
        return "users/fragments/table-row :: user-row";
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @HxRequest
    @HxTrigger("user-deleted")
    public String delete(@PathVariable UUID id, Model model) {
        try {
            userService.delete(id);
            return "common/fragments/empty :: empty";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
    }

    /**
     * 重置密码模态框
     */
    @GetMapping("/{id}/reset-password/modal")
    @HxRequest
    public String resetPasswordModal(@PathVariable UUID id, Model model) {
        var user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        model.addAttribute("user", user);
        return "users/fragments/reset-password-modal :: reset-password-modal";
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    @HxRequest
    @HxTrigger("password-reset")
    public String resetPassword(
            @PathVariable UUID id,
            @RequestParam String newPassword,
            Model model) {
        
        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("error", "密码长度不能少于6个字符");
            var user = userService.findById(id).orElseThrow();
            model.addAttribute("user", user);
            return "users/fragments/reset-password-modal :: reset-password-modal";
        }
        
        userService.resetPassword(id, newPassword);
        model.addAttribute("success", "密码重置成功");
        return "common/fragments/success :: success-toast";
    }
}

