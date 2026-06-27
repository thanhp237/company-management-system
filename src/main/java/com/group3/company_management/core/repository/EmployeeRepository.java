package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUser_Username(String username);

    List<Employee> findByEmployeeType(String employeeType);
}