package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.RoleSummaryDTO;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.repository.RoleRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private static final Set<String> SYSTEM_ROLES = Set.of(
            "ADMIN",
            "CEO",
            "DIRECTOR",
            "SALES",
            "SALES_MANAGER",
            "ADMIN_OFFICER",
            "ACCOUNTANT",
            "MARKETING"
    );

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummaryDTO> getRoleSummaries() {
        List<Role> roles = roleRepository.findAllByOrderByIdAsc();
        List<RoleSummaryDTO> summaries = new ArrayList<>();
        
        long stt = 1;
        for (Role role : roles) {
            Long totalCount = userRepository.countActiveUsersByRoleCode(role.getRoleCode());
            
            RoleSummaryDTO dto = new RoleSummaryDTO(
                role.getId(),
                stt++,
                role.getRoleCode(),
                role.getRoleName(),
                normalizeStatus(role.getStatus()),
                totalCount != null ? totalCount : 0L,
                isSystemRole(role.getRoleCode()),
                canDelete(role, totalCount)
            );
            summaries.add(dto);
        }
        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleForm(Long id) {
        if (id == null) {
            Role role = new Role();
            role.setStatus("ACTIVE");
            return role;
        }

        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    }

    @Override
    public void saveRole(Role role) {
        String roleCode = normalizeRoleCode(role.getRoleCode());
        String roleName = normalizeRequired(role.getRoleName(), "Role name is required");

        boolean duplicated = role.getId() == null
                ? roleRepository.existsByRoleCodeIgnoreCase(roleCode)
                : roleRepository.existsByRoleCodeIgnoreCaseAndIdNot(roleCode, role.getId());

        if (duplicated) {
            throw new IllegalArgumentException("Role code already exists");
        }

        Role target = role.getId() == null
                ? new Role()
                : roleRepository.findById(role.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (target.getId() != null && isSystemRole(target.getRoleCode()) && !target.getRoleCode().equals(roleCode)) {
            throw new IllegalArgumentException("System role code cannot be changed");
        }

        target.setRoleCode(roleCode);
        target.setRoleName(roleName);
        target.setStatus(normalizeStatus(role.getStatus()));
        roleRepository.save(target);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        role.setStatus(normalizeStatus(status));
        roleRepository.save(role);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Long userCount = userRepository.countActiveUsersByRoleCode(role.getRoleCode());
        if (isSystemRole(role.getRoleCode())) {
            throw new IllegalArgumentException("System roles cannot be deleted. Please deactivate instead.");
        }
        if (userCount != null && userCount > 0) {
            throw new IllegalArgumentException("Role is assigned to users. Please deactivate instead.");
        }

        roleRepository.delete(role);
    }

    @Override
    public boolean isSystemRole(String roleCode) {
        if (roleCode == null) {
            return false;
        }
        return SYSTEM_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private boolean canDelete(Role role, Long totalCount) {
        return !isSystemRole(role.getRoleCode()) && (totalCount == null || totalCount == 0);
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = normalizeRequired(roleCode, "Role code is required")
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');

        if (!normalized.matches("[A-Z0-9_]{2,30}")) {
            throw new IllegalArgumentException("Role code must be 2-30 characters and only contain A-Z, 0-9 or underscore");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported role status: " + status);
        }
        return normalized;
    }
}
