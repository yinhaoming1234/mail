package com.yhm.adminweb.dto;

import com.yhm.adminweb.entity.MailDomain;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 域名 DTO
 * 用于 API 响应，只返回客户端需要的域名信息
 * Requirements: 7.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainDto {
    
    private String id;
    private String domain;
    private String description;
    
    /**
     * 从实体转换为 DTO
     */
    public static DomainDto fromEntity(MailDomain entity) {
        return DomainDto.builder()
                .id(entity.getId().toString())
                .domain(entity.getDomain())
                .description(entity.getDescription())
                .build();
    }
}
