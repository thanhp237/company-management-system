package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.ProfileUpdateRequest;
import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.DepartmentRepository;
import com.group3.company_management.core.service.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();

    }
    private Department findActiveDepartmentById(Long id) {
        return departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));
    }


    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Override
    @Transactional
    public void saveDepartment(Department department) {
        if (departmentRepository.existsByCode(department.getCode())) {
            throw new RuntimeException("Department code already exists");
        }
        String code = validate(department.getCode(),"Code not blank");
        String name = validate(department.getName(),"Name not blank");

        departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id, String username) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        department.setIsDeleted(true);
        department.setDeletedAt(LocalDateTime.now());
        department.setDeletedBy(username);

        departmentRepository.save(department);
    }

    @Override
    public Page<Department> searchByIdandName(String keyword,String status,int page) {
        Pageable pageable = PageRequest.of(page,10,Sort.by("id").ascending());
        if (keyword == null) {
            keyword = "";
        }

        if (status == null || status.trim().isEmpty()) {
            status = "all";
        }
        return departmentRepository.search(keyword,status,pageable);
    }
    private String validate(String  value, String mes){
        if(value == null || value.trim().isEmpty()){
            throw new RuntimeException(mes);
        }
        return value.trim();
    }
    @Override
    public Page<Department> getDepartmentsPage(int page) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").ascending());

        return departmentRepository.findAllNotDeleted(pageable);
    }





    @Override
    public void updateDepartmentStatus(Long id, String status) {
        if (id == null) {
            throw new IllegalArgumentException("User ID is required");
        }


        Department department1 = findActiveDepartmentById(id);
        department1.setStatus(status);
        departmentRepository.save(department1);
    }

}