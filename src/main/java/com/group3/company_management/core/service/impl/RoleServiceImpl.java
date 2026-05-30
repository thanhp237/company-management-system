package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.RoleSummaryDTO;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.repository.RoleRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<RoleSummaryDTO> getRoleSummaries() {
        List<Role> roles = roleRepository.findAll();
        List<RoleSummaryDTO> summaries = new ArrayList<>();
        
        long stt = 1;
        for (Role role : roles) {
            // Gọi UserRepository để đếm số user đang hoạt động thuộc role này
            Long totalCount = userRepository.countActiveUsersByRoleCode(role.getRoleCode());
            
            RoleSummaryDTO dto = new RoleSummaryDTO(
                stt++,
                role.getRoleCode(),
                role.getRoleName(),
                totalCount != null ? totalCount : 0L
            );
            summaries.add(dto);
        }
        return summaries;
    }
}