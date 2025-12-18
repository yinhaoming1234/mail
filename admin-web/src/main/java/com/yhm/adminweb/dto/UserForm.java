package com.yhm.adminweb.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * 用户表单 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForm {
    
    private UUID id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 64, message = "用户名长度必须在1-64个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "用户名只能包含字母、数字、点、下划线和连字符")
    private String username;
    
    @NotBlank(message = "请选择域名")
    private String domain;
    
    @Size(min = 6, max = 255, message = "密码长度必须在6-255个字符之间")
    private String password;
    
    @Min(value = 1048576, message = "配额不能小于 1MB")
    @Max(value = 107374182400L, message = "配额不能大于 100GB")
    private Long quotaBytes;
    
    private Boolean isEnabled;
    
    /**
     * 构建邮箱地址
     */
    public String buildEmail() {
        return username + "@" + domain;
    }
}

