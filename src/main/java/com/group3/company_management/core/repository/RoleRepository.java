package com.group3.company_management.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group3.company_management.core.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
}
