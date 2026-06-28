package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.DepartmentRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAssignableUsers(Long departmentId) {
        return userRepository.findAssignableUsersForDepartment(departmentId);
    }
    private Department findActiveDepartmentById(Long id) {
        return departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));
    }


    @Override
    public List<Department> getAllDepartments() {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }
    @Override
    @Transactional
    public void addUsersToDepartment(List<Long> userIds, Long departmentId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Department dept = departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (!"ACTIVE".equalsIgnoreCase(dept.getStatus())) {
            throw new RuntimeException("Không thể gán nhân viên vào phòng ban đã ngừng hoạt động");
        }

        List<User> users = userRepository.findAllById(userIds);

        List<User> newUsers = users.stream()
                .filter(User::isActive)
                .filter(user -> !user.isAdmin())
                .filter(user -> !departmentId.equals(user.getDepartmentId()))
                .toList();

        int currentSize = userRepository.countByDepartmentIdAndIsDeletedFalse(departmentId);

        if (dept.getMaxMembers() != null && currentSize + newUsers.size() > dept.getMaxMembers()) {
            throw new RuntimeException(
                    "Vượt quá giới hạn phòng ban. Hiện có " + currentSize
                            + ", chọn thêm " + newUsers.size()
                            + ", tối đa " + dept.getMaxMembers()
            );
        }

        for (User user : newUsers) {
            user.setDepartmentId(dept.getId());
        }

        userRepository.saveAll(newUsers);
    }

    @Override
    @Transactional
    public Department saveDepartment(Department department) {
        department.setCode(validate(department.getCode(), "Code is required"));
        department.setName(validate(department.getName(), "Name is required"));

        boolean duplicateCode = department.getId() == null
                ? departmentRepository.existsByCode(department.getCode())
                : departmentRepository.existsByCodeAndIdNot(department.getCode(), department.getId());

        if (duplicateCode) {
            throw new RuntimeException("Department code already exists");
        }
        if (department.getMaxMembers() != null && department.getMaxMembers() < 1) {
            throw new RuntimeException("Max members must be greater than 0");
        }

        if (department.getId() != null && department.getMaxMembers() != null) {
            int currentMembers = userRepository.countByDepartmentIdAndIsDeletedFalse(department.getId());
            if (department.getMaxMembers() < currentMembers) {
                throw new RuntimeException("Max members cannot be less than current members (" + currentMembers + ")");
            }
        }

        if (department.getDescription() != null) {
            department.setDescription(department.getDescription().trim());
        }

        if (department.getStatus() == null || department.getStatus().trim().isEmpty()) {
            department.setStatus("ACTIVE");
        }

        return departmentRepository.save(department);
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
    @Transactional
    public void updateDepartmentStatus(Long id, String status) {
        if (id == null) {
            throw new IllegalArgumentException("Department ID is required");
        }

        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new RuntimeException("Invalid department status");
        }

        Department department = findActiveDepartmentById(id);
        department.setStatus(status.toUpperCase());
        departmentRepository.save(department);
    }
    @Override
    @Transactional(readOnly = true)
    public int countMembers(Long departmentId) {
        if (departmentId == null) {
            return 0;
        }
        return userRepository.countByDepartmentIdAndIsDeletedFalse(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> countMembersByDepartmentIds(Collection<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.countActiveUsersByDepartmentIds(departmentIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }



}
