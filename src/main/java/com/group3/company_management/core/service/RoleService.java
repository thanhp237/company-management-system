package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.RoleSummaryDTO;
import com.group3.company_management.core.entity.Role;

import java.util.List;

public interface RoleService {
    List<RoleSummaryDTO> getRoleSummaries();

    Role getRoleForm(Long id);

    void saveRole(Role role);

    void updateStatus(Long id, String status);

    void deleteRole(Long id);

    boolean isSystemRole(String roleCode);
}
