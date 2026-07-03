package com.group3.company_management.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group3.company_management.core.entity.Role;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByOrderByIdAsc();

    List<Role> findByStatusIgnoreCaseOrderByRoleNameAsc(String status);

    boolean existsByRoleCodeIgnoreCase(String roleCode);

    boolean existsByRoleCodeIgnoreCaseAndIdNot(String roleCode, Long id);
}
