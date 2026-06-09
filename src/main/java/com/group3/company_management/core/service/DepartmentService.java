package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Department;
import java.util.List;

public interface DepartmentService {
        List<Department> getAllDepartments();

        Department getDepartmentById(Long id);

        void saveDepartment(Department department);

        void deleteDepartment(Long id);
        List<Department> searchByIdandName(String keyword);
}