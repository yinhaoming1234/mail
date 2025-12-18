package com.yhm.adminweb.controller;

import com.yhm.adminweb.dto.PasswordChangeRequest;
import com.yhm.adminweb.dto.UserForm;
import com.yhm.adminweb.entity.MailDomain;
import com.yhm.adminweb.entity.User;
import com.yhm.adminweb.service.MailDomainService;
import com.yhm.adminweb.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API 控制器
 * 提供给移动客户端使用的接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 允许跨域访问
public class ApiController {

    private final UserService userService;
    private final MailDomainService domainService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     */
    @PostMapping("/users/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = request.get("username");
            String domain = request.get("domain");
            String password = request.get("password");

            // 验证参数
            if (username == null || username.isBlank()) {
                response.put("success", false);
                response.put("message", "用户名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            if (domain == null || domain.isBlank()) {
                response.put("success", false);
                response.put("message", "域名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            if (password == null || password.length() < 6) {
                response.put("success", false);
                response.put("message", "密码长度至少6个字符");
                return ResponseEntity.badRequest().body(response);
            }

            // 创建用户
            UserForm form = new UserForm();
            form.setUsername(username);
            form.setDomain(domain);
            form.setPassword(password);
            form.setIsEnabled(true);
            form.setIsAdmin(false);
            form.setQuotaBytes(1073741824L); // 1GB

            User user = userService.create(form);

            response.put("success", true);
            response.put("message", "注册成功");
            response.put("email", user.getEmail());
            
            log.info("New user registered: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Registration failed", e);
            response.put("success", false);
            response.put("message", "注册失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 用户登录验证
     */
    @PostMapping("/users/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String password = request.get("password");

            if (email == null || email.isBlank()) {
                response.put("success", false);
                response.put("message", "邮箱不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            if (password == null || password.isBlank()) {
                response.put("success", false);
                response.put("message", "密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 查找用户
            var userOpt = userService.findByEmail(email.toLowerCase());
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            User user = userOpt.get();
            
            // 检查用户是否启用
            if (!user.getIsEnabled()) {
                response.put("success", false);
                response.put("message", "账户已被禁用");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 验证密码 - 支持明文密码（开发测试）和加密密码
            boolean passwordMatch = passwordEncoder.matches(password, user.getPasswordHash()) 
                    || password.equals(user.getPasswordHash());
            
            if (!passwordMatch) {
                response.put("success", false);
                response.put("message", "密码错误");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            response.put("domain", user.getDomain());
            response.put("isAdmin", user.getIsAdmin());
            
            log.info("User logged in: {}", user.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Login failed", e);
            response.put("success", false);
            response.put("message", "登录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/users/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            if (email == null || email.isBlank()) {
                response.put("success", false);
                response.put("message", "邮箱不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            if (currentPassword == null || currentPassword.isBlank()) {
                response.put("success", false);
                response.put("message", "当前密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            if (newPassword == null || newPassword.length() < 6) {
                response.put("success", false);
                response.put("message", "新密码长度至少6个字符");
                return ResponseEntity.badRequest().body(response);
            }

            userService.changePassword(email, currentPassword, newPassword);

            response.put("success", true);
            response.put("message", "密码修改成功");
            
            log.info("Password changed for user: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Change password failed", e);
            response.put("success", false);
            response.put("message", "密码修改失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取可用域名列表
     */
    @GetMapping("/domains")
    public ResponseEntity<List<Map<String, Object>>> getDomains() {
        try {
            List<MailDomain> domains = domainService.findEnabled();
            List<Map<String, Object>> result = domains.stream()
                    .map(d -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("domain", d.getDomain());
                        map.put("description", d.getDescription());
                        return map;
                    })
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Get domains failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/users/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            var userOpt = userService.findByEmail(email.toLowerCase());
            response.put("available", userOpt.isEmpty());
            response.put("email", email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("available", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
