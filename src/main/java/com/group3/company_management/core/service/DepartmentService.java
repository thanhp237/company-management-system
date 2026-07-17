package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.DepartmentRequest;
import com.group3.company_management.core.dto.DepartmentResponse;
import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
@Service
public interface DepartmentService {

        DepartmentRequest getDepartmentById(Long id);
       void saveDepartment(DepartmentRequest departmentRequest,String name);
    void deleteDepartment(Long id, String username);
    public Page<DepartmentResponse> search(String keyword, String status, Pageable pageable);
    public void updateDepartmentStatus(Long id, String status);

































    List<Department> getAllDepartments();
//
//        Department getDepartmentById(Long id);
//
//        Department saveDepartment(Department department);
//        public void deleteDepartment(Long id, String username);
//        Page<Department> searchByIdandName(String keyword,String status,int page);
//        Page<Department> getDepartmentsPage(int page);
//
//        public void updateDepartmentStatus(Long id, String status);
//
//        int countMembers(Long departmentId);
//
//        Map<Long, Integer> countMembersByDepartmentIds(List<Long> departmentIds);
//        public void addUsersToDepartment(List<Long> userIds, Long departmentId);
//        List<User> getAssignableUsers(Long departmentId) ;
//        List<User> getUsersInDepartment(Long departmentId);
//
//        User getDepartmentManager(Long managerId);
}