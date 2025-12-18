package com.yhm.adminweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 群发邮件表单 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastEmailForm {
    
    @NotBlank(message = "主题不能为空")
    @Size(max = 998, message = "主题长度不能超过998个字符")
    private String subject;
    
    @NotBlank(message = "邮件内容不能为空")
    private String body;
    
    /**
     * 目标域名（可选，为空则发送给所有用户）
     */
    private String targetDomain;
    
    /**
     * 指定收件人列表（可选）
     */
    private List<String> recipients;
    
    /**
     * 是否只发送给管理员
     */
    private Boolean adminOnly = false;
}
