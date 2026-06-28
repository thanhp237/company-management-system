package com.group3.company_management.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSummaryDTO {
    private Long id;
    private Long stt;
    private String roleCode;
    private String roleName;
    private String status;
    private Long totalCount;
    private boolean systemRole;
    private boolean canDelete;
}
