package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DepartmentService {
        List<Department> getAllDepartments();

        Department getDepartmentById(Long id);

        Department saveDepartment(Department department);
        public void deleteDepartment(Long id, String username);
        Page<Department> searchByIdandName(String keyword,String status,int page);
        Page<Department> getDepartmentsPage(int page);

        public void updateDepartmentStatus(Long id, String status);

        int countMembers(Long departmentId);

        Map<Long, Integer> countMembersByDepartmentIds(Collection<Long> departmentIds);
        public void addUsersToDepartment(List<Long> userIds, Long departmentId);
        List<User> getAssignableUsers(Long departmentId) ;
}