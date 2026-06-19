package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    @Query("select d from Department d where d.name like concat('%', :keyword, '%') or d.code like concat('%', :keyword, '%')")
    List<Department> search(@Param("keyword") String keyword);
    boolean existsByCode(String code);
    Optional<Department> findByName(String name);
}