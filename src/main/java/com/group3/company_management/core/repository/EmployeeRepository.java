package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUser_Username(String username);

    List<Employee> findByEmployeeType(String employeeType);

    @Query("""
            select e
            from Employee e
            join fetch e.user u
            join fetch u.role r
            where upper(r.roleCode) = 'SALES'
            and u.isDeleted = false
            order by u.fullName asc
            """)
    List<Employee> findActiveSalesEmployees();
}