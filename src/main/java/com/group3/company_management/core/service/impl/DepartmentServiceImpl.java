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
    public void updateStatus(Long id, String status) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        department.setStatus(status);

        departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
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
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    @Override
    public List<Department> searchByIdandName(String keyword) {
         if(departmentRepository.search(keyword)== null || departmentRepository.search(keyword).isEmpty()){
             throw new RuntimeException("Department not found");
         }if(keyword == null || keyword.trim().isEmpty()){
             throw  new RuntimeException("Keyword not blank");
        }else{
        return departmentRepository.search(keyword);}
    }
    private String validate(String  value, String mes){
        if(value == null || value.trim().isEmpty()){
            throw new RuntimeException(mes);
        }
        return value.trim();
    }


    @Override

   public Page<Department> getDepartmentsPage(int page){
        Pageable pageable = PageRequest.of(
                page,
                5,
                Sort.by("id").ascending()
        );
        return departmentRepository.findAll(pageable);
    }
    @Override
    public void updateDepartmentStatus(Long id, String status) {
        if (id == null) {
            throw new IllegalArgumentException("User ID is required");
        }


        Department department1 = findActiveUserById(id);
        department1.setStatus(status);
        departmentRepository.save(department1);
    }
    private Department findActiveUserById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }
}