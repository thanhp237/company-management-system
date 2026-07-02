package com.group3.company_management.core.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;

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
    @Query("""
    select u
    from User u
    where u.role.roleCode in :roles
    and u.isDeleted = false
""")
    List<User> findUsersByRoleNames(@Param("roles") List<String> roles);
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


    List<User> findByRole_RoleName(String roleName);



    @EntityGraph(attributePaths = {"role", "employee"})

    @Query("SELECT u FROM User u WHERE u.role.roleCode = :roleCode AND u.isDeleted = false")
    Page<User> findActiveUsersByRoleCode(@Param("roleCode") String roleCode, Pageable pageable);

    @EntityGraph(attributePaths = {"role", "employee"})
    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    Page<User> findAllActiveWithEmployee(Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND LOWER(u.status) = LOWER(:status)
            """)
    List<User> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") String status
    );

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<User> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE LOWER(u.status) = LOWER(:status)")
    List<User> searchByStatus(@Param("status") String status);

    @EntityGraph(attributePaths = {"role", "employee"})
    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status IS NULL OR :status = '' OR LOWER(u.status) = LOWER(:status))
        AND (:roleCode IS NULL OR :roleCode = '' OR LOWER(u.role.roleCode) = LOWER(:roleCode))
        """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("roleCode") String roleCode,
            Pageable pageable
    );

    @Query(value = "SELECT employee_code FROM employees WHERE account_id = :accountId", nativeQuery = true)
    String findEmployeeCodeByAccountId(@Param("accountId") Long accountId);
    int countByDepartmentIdAndIsDeletedFalse(Long departmentId);




    List<User> findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(Long departmentId);
    Optional<User> findCustomerById(Long id);
    @Query("select u.fullName from User u where u.id=:id")
    String getNameUserById(@Param("id") Long id);



}
