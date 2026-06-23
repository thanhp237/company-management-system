package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.entity.Department;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DepartmentService {
        List<Department> getAllDepartments();

        Department getDepartmentById(Long id);

        void saveDepartment(Department department);

        public void deleteDepartment(Long id, String username);
        Page<Department> searchByIdandName(String keyword,String status,int page);
        Page<Department> getDepartmentsPage(int page);

        public void updateDepartmentStatus(Long id, String status);

}