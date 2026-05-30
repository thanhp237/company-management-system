package com.group3.company_management.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSummaryDTO {
    private Long stt;
    private String roleCode;
    private String roleName;
    private Long totalCount;
}