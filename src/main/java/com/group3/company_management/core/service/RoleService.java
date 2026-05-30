package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.RoleSummaryDTO;
import java.util.List;

public interface RoleService {
    List<RoleSummaryDTO> getRoleSummaries();
}