package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.DepartmentRequest;
import com.group3.company_management.core.dto.DepartmentResponse;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.DepartmentRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.DepartmentService;
import com.group3.company_management.core.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
   private final UserService userService;


    public DepartmentServiceImpl(DepartmentRepository departmentRepository, UserRepository userRepository, UserService userService) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override

    public Page<DepartmentResponse> getAllDerpartment(Pageable pageable) {


        return departmentRepository.findAll(pageable)
                .map(this::toResponse);
    }
    @Override
   public DepartmentRequest getDepartmentById(Long id){
        Department department=  departmentRepository.findByIdAndIsDeletedFalse(id).orElseThrow(() ->  new RuntimeException("Department not found"));
        DepartmentRequest departmentRequest = new DepartmentRequest();
        departmentRequest.setId(department.getId());
        departmentRequest.setCode(department.getCode());
        departmentRequest.setName((department.getName()));
        departmentRequest.setDescription((department.getDescription()));
        departmentRequest.setManagerId(department.getManagerId());
        departmentRequest.setStatus(department.getStatus());
        departmentRequest.setMaxMembers(department.getMaxMembers());
        departmentRequest.setManagerName(userRepository.getNameUserById(departmentRequest.getManagerId()));
        departmentRequest.setEmployees(userService.getUsersByRoles(List.of("SALES","ACCOUNTANT","MARKETING")));
        return  departmentRequest;
    }

    @Override

    public void saveDepartment(DepartmentRequest departmentRequest, String name) {
        if (departmentRequest.getCode() == null || departmentRequest.getCode().trim().isEmpty()) {
            throw new RuntimeException("Code không được để trống");
        }
        if (departmentRequest.getName() == null || departmentRequest.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên phòng ban không được để trống");
        }

        if (departmentRequest.getManagerId() == null) {
            throw new RuntimeException("Vui lòng chọn quản lý");

        }


        if (departmentRequest.getMaxMembers() == null || departmentRequest.getMaxMembers() <= 0) {
            throw new RuntimeException("Số lượng tối đa phải lớn hơn 0");
        }
        if (departmentRequest.getEmployees() != null
                && departmentRequest.getEmployees().size() > departmentRequest.getMaxMembers()) {

            throw new RuntimeException("Số nhân viên được chọn vượt quá số lượng tối đa.");
        }
        if (departmentRequest.getId() == null) {
            if (departmentRepository.existsByCode(departmentRequest.getCode())) {
                throw new RuntimeException("Code phòng ban đã tồn tại");
            }

        } else {


            if (departmentRepository.existsByCodeAndIdNot(
                    departmentRequest.getCode(),
                    departmentRequest.getId())) {

                throw new RuntimeException("Code phòng ban đã tồn tại");

            }
        }

        Department department;

        if (departmentRequest.getId() == null) {

            department = new Department();
            department.setCreatedAt(LocalDateTime.now());

        } else {

            department = departmentRepository.findByIdAndIsDeletedFalse(departmentRequest.getId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            department.setUpdatedAt(LocalDateTime.now());
        }

        department.setCode(departmentRequest.getCode().trim());
        department.setName(departmentRequest.getName().trim());
        department.setDescription(departmentRequest.getDescription().trim());
        department.setManagerId(departmentRequest.getManagerId());
        department.setMaxMembers(departmentRequest.getMaxMembers());
        department.setStatus(departmentRequest.getStatus());

        Department savedDepartment = departmentRepository.save(department);
        List<User> oldUsers =
                userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(savedDepartment.getId());

        for (User user : oldUsers) {
            user.setDepartmentId(null);
        }
        userRepository.saveAll(oldUsers);
        if (departmentRequest.getEmployeeIds() != null
                && !departmentRequest.getEmployeeIds().isEmpty()) {

            List<User> users =
                    userRepository.findAllById(departmentRequest.getEmployeeIds());

            for (User user : users) {
                user.setDepartmentId(savedDepartment.getId());
            }

            userRepository.saveAll(users);
        }
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id, String username) {





            Department department = departmentRepository
                    .findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            List<User> users =
                    userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(id);
            for (User user : users) {
                user.setDepartmentId(null);
            }
            userRepository.saveAll(users);
            department.setIsDeleted(true);
            department.setDeletedAt(LocalDateTime.now());
            department.setDeletedBy(username);

            departmentRepository.save(department);

    }
    @Override
    @Transactional
    public void updateDepartmentStatus(Long id, String status) {
        if (id == null) {
            throw new IllegalArgumentException("Thiếu mã phòng ban");
        }

        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new RuntimeException("Trạng thái phòng ban không hợp lệ");
        }

        Department    department = departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        department.setStatus(status.toUpperCase());
        departmentRepository.save(department);
    }
    @Override
    public Page<DepartmentResponse> search(String keyword,
                                           String status,
                                           Pageable pageable) {

        return departmentRepository
                .search(keyword, status, pageable)
                .map(this::toResponse);
    }


    private DepartmentResponse toResponse(Department department){
        String name = userRepository.getNameUserById(department.getManagerId());
        int soEmployee = userRepository.countByDepartmentIdAndIsDeletedFalse(department.getId());
        DepartmentResponse response=DepartmentResponse.fromEntity(department,name,soEmployee);
        return response;
    }
    private String nameDepartment(Long id){

        return userRepository.getNameUserById(id);

    }





















































//    @Override
//    @Transactional(readOnly = true)
//    public List<User> getAssignableUsers(Long departmentId) {
//        return userRepository.findAssignableUsersForDepartment(departmentId);
//    }
//
//
//
    @Override
    public List<Department> getAllDepartments() {
        return List.of();
    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Department getDepartmentById(Long id) {
//        return departmentRepository.findByIdAndIsDeletedFalse(id)
//                .orElseThrow(() -> new RuntimeException("Department not found"));
//    }
//    @Override
//    @Transactional
//    public void addUsersToDepartment(List<Long> userIds, Long departmentId) {
//        if (userIds == null || userIds.isEmpty()) {
//            return;
//        }
//
//        Department dept = departmentRepository.findByIdAndIsDeletedFalse(departmentId)
//                .orElseThrow(() -> new RuntimeException("Department not found"));
//
//        if (!"ACTIVE".equalsIgnoreCase(dept.getStatus())) {
//            throw new RuntimeException("Không thể gán nhân viên vào phòng ban đã ngừng hoạt động");
//        }
//
//        List<User> users = userRepository.findAllById(userIds);
//
//        List<User> newUsers = new ArrayList<>();
//
//        for (User user : users) {
//            if (user.isActive()
//                    && !user.isAdmin()
//                    && !departmentId.equals(user.getDepartmentId())) {
//
//                newUsers.add(user);
//            }
//        }
//        int currentSize = userRepository.countByDepartmentIdAndIsDeletedFalse(departmentId);
//
//        if (dept.getMaxMembers() != null && currentSize + newUsers.size() > dept.getMaxMembers()) {
//            throw new RuntimeException(
//                    "Vượt quá giới hạn phòng ban. Hiện có " + currentSize
//                            + ", chọn thêm " + newUsers.size()
//                            + ", tối đa " + dept.getMaxMembers()
//            );
//        }
//
//        for (User user : newUsers) {
//            user.setDepartmentId(dept.getId());
//        }
//
//        userRepository.saveAll(newUsers);
//    }
//
//    @Override
//    @Transactional
//    public Department saveDepartment(Department department) {
//        department.setCode(validate(department.getCode(), "Code is required"));
//        department.setName(validate(department.getName(), "Name is required"));
//
//        boolean duplicateCode;
//
//        if (department.getId() == null) {
//            duplicateCode = departmentRepository.existsByCode(department.getCode());
//        } else {
//            duplicateCode = departmentRepository.existsByCodeAndIdNot(
//                    department.getCode(),
//                    department.getId()
//            );
//        }
//
//        if (duplicateCode) {
//            throw new RuntimeException("Department code already exists");
//        }
//        if (department.getMaxMembers() != null && department.getMaxMembers() < 1) {
//            throw new RuntimeException("Max members must be greater than 0");
//        }
//
//        if (department.getId() != null && department.getMaxMembers() != null) {
//            int currentMembers = userRepository.countByDepartmentIdAndIsDeletedFalse(department.getId());
//            if (department.getMaxMembers() < currentMembers) {
//                throw new RuntimeException("Max members cannot be less than current members (" + currentMembers + ")");
//            }
//        }
//
//        if (department.getDescription() != null) {
//            department.setDescription(department.getDescription().trim());
//        }
//
//        if (department.getStatus() == null || department.getStatus().trim().isEmpty()) {
//            department.setStatus("ACTIVE");
//        }
//
//        return departmentRepository.save(department);
//    }
//
//    @Override
//    @Transactional
//    public void deleteDepartment(Long id, String username) {
//
//        Department department = departmentRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Department not found"));
//
//        department.setIsDeleted(true);
//        department.setDeletedAt(LocalDateTime.now());
//        department.setDeletedBy(username);
//
//        departmentRepository.save(department);
//    }
//    @Override
//    @Transactional(readOnly = true)
//    public List<User> getUsersInDepartment(Long departmentId) {
//        if (departmentId == null) {
//            return List.of();
//        }
//
//        return userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(departmentId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public User getDepartmentManager(Long managerId) {
//        if (managerId == null) {
//            return null;
//        }
//
//        return userRepository.findById(managerId).orElse(null);
//    }
//
//    @Override
//    public Page<Department> searchByIdandName(String keyword,String status,int page) {
//        Pageable pageable = PageRequest.of(page,10,Sort.by("id").ascending());
//        if (keyword == null) {
//            keyword = "";
//        }
//
//        if (status == null || status.trim().isEmpty()) {
//            status = "all";
//        }
//        return departmentRepository.search(keyword,status,pageable);
//    }
//    private String validate(String  value, String mes){
//        if(value == null || value.trim().isEmpty()){
//            throw new RuntimeException(mes);
//        }
//        return value.trim();
//    }
//    private Department findActiveDepartmentById(Long id) {
//        return departmentRepository.findByIdAndIsDeletedFalse(id)
//                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));
//    }
//    @Override
//    public Page<Department> getDepartmentsPage(int page) {
//
//        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").ascending());
//
//        return departmentRepository.findAllNotDeleted(pageable);
//    }
//
//
//
//
//
//    @Override
//    @Transactional
//    public void updateDepartmentStatus(Long id, String status) {
//        if (id == null) {
//            throw new IllegalArgumentException("Department ID is required");
//        }
//
//        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
//            throw new RuntimeException("Invalid department status");
//        }
//
//        Department department = findActiveDepartmentById(id);
//        department.setStatus(status.toUpperCase());
//        departmentRepository.save(department);
//    }
//    @Override
//    @Transactional(readOnly = true)
//    public int countMembers(Long departmentId) {
//        if (departmentId == null) {
//            return 0;
//        }
//        return userRepository.countByDepartmentIdAndIsDeletedFalse(departmentId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Map<Long, Integer> countMembersByDepartmentIds(List<Long> departmentIds) {
//
//        if (departmentIds == null || departmentIds.isEmpty()) {
//            return Map.of();
//        }
//
//        List<Object[]> rows = userRepository.countActiveUsersByDepartmentIds(departmentIds);
//
//        Map<Long, Integer> result = new HashMap<>();
//
//        for (Object[] row : rows) {
//            Long departmentId = (Long) row[0];
//            Integer memberCount = ((Number) row[1]).intValue();
//
//            result.put(departmentId, memberCount);
//        }
//
//        return result;
//    }



}
