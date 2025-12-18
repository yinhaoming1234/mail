package com.yhm.adminweb.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * 域名表单 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainForm {
    
    private UUID id;
    
    @NotBlank(message = "域名不能为空")
    @Size(min = 1, max = 255, message = "域名长度必须在1-255个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$", 
             message = "域名格式不正确")
    private String domain;
    
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    
    private Boolean isLocal;
    
    private Boolean isEnabled;
}

