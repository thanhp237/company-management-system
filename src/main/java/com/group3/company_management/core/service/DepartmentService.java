package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.entity.Department;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DepartmentService {
        List<Department> getAllDepartments();

        Department getDepartmentById(Long id);

        void saveDepartment(Department department);

        void deleteDepartment(Long id);
        List<Department> searchByIdandName(String keyword);
        Page<Department> getDepartmentsPage(int page);
        public void updateStatus(Long id, String status);
        public void updateDepartmentStatus(Long id, String status);

}