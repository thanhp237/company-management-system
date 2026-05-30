package com.group3.company_management.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.group3.company_management.core.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find user by username for login
    Optional<User> findByUsername(String username);
    
    // Find user by email
    Optional<User> findByEmail(String email);
    
    // Find user by username, excluding soft-deleted users
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
    Optional<User> findByUsernameAndNotDeleted(@Param("username") String username);

    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalseAndIdNot(String username, Long id);

    boolean existsByEmailAndIsDeletedFalseAndIdNot(String email, Long id);

    // 1. Hàm đếm số lượng User theo từng RoleCode (Dùng cho trang danh sách Role)
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleCode = :roleCode AND u.isDeleted = false")
    Long countActiveUsersByRoleCode(@Param("roleCode") String roleCode);

    // 2. Hàm lấy danh sách User theo RoleCode (Dùng cho tính năng lọc của nút Detail)
    @Query("SELECT u FROM User u WHERE u.role.roleCode = :roleCode AND u.isDeleted = false")
    List<User> findActiveUsersByRoleCode(@Param("roleCode") String roleCode);
}
