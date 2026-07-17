package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.repository.CustomerActivityRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.service.CustomerActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerActivityServiceImpl
        implements CustomerActivityService {

    private final CustomerActivityRepository repository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    private List<Long> getAccessibleEmployeeIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }
        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User currentUser = userOpt.get();

        // 1. ADMIN & DIRECTOR see all
        if (currentUser.isAdmin() || "DIRECTOR".equalsIgnoreCase(currentUser.getRole().getRoleCode())) {
            return null;
        }

        // 2. Trưởng phòng (MANAGER / SALES_MANAGER) sees department members' activities (excluding manager role)
        if (currentUser.isManager() || "SALES_MANAGER".equalsIgnoreCase(currentUser.getRole().getRoleCode())) {
            Long deptId = currentUser.getDepartmentId();
            if (deptId != null) {
                List<User> deptUsers = userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(deptId);
                List<Long> employeeIds = new ArrayList<>();
                for (User u : deptUsers) {
                    if (u.getEmployee() != null && 
                        !"MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) && 
                        !"SALES_MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) &&
                        !"ADMIN".equalsIgnoreCase(u.getRole().getRoleCode())) {
                        employeeIds.add(u.getEmployee().getId());
                    }
                }
                return employeeIds;
            }
        }

        // 3. Normal Sales rep sees only their own activities
        Optional<Employee> empOpt = employeeRepository.findByUser_Username(username);
        if (empOpt.isPresent()) {
            return List.of(empOpt.get().getId());
        }

        return List.of();
    }

    @Override
    public Page<CustomerActivity> getActivities(
            String activityType,
            int page,
            int size) {

        List<Long> employeeIds = getAccessibleEmployeeIds();
        Pageable pageable = activityPageable(page, size);

        if (employeeIds == null) {
            if (activityType == null || activityType.isBlank()) {
                return repository.findAll(pageable);
            }
            return repository.findByActivityTypeIgnoreCase(activityType, pageable);
        } else {
            if (employeeIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (activityType == null || activityType.isBlank()) {
                return repository.findByEmployeeIdIn(employeeIds, pageable);
            }
            return repository.findByEmployeeIdInAndActivityTypeIgnoreCase(employeeIds, activityType, pageable);
        }
    }

    @Override
    public Page<CustomerActivity> getActivities(
            Long customerId,
            String activityType,
            int page,
            int size) {

        if (customerId == null) {
            return getActivities(activityType, page, size);
        }

        List<Long> employeeIds = getAccessibleEmployeeIds();
        Pageable pageable = activityPageable(page, size);

        if (employeeIds == null) {
            if (activityType == null || activityType.isBlank()) {
                return repository.findByCustomerId(customerId, pageable);
            }
            return repository.findByCustomerIdAndActivityTypeIgnoreCase(customerId, activityType, pageable);
        } else {
            if (employeeIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (activityType == null || activityType.isBlank()) {
                return repository.findByEmployeeIdInAndCustomerId(employeeIds, customerId, pageable);
            }
            return repository.findByEmployeeIdInAndCustomerIdAndActivityTypeIgnoreCase(employeeIds, customerId, activityType, pageable);
        }
    }

    @Override
    public CustomerActivity getActivityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tương tác."));
    }

    @Override
    public void updateActivityNote(Long id,
                                   String activityNote) {

        CustomerActivity activity =
                repository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy tương tác."));

        activity.setActivityNote(activityNote);

        repository.save(activity);
    }

    @Override
    public List<CustomerActivity> getActivitiesByCustomerId(
            Long customerId) {

        List<Long> employeeIds = getAccessibleEmployeeIds();
        if (employeeIds == null) {
            return repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else {
            if (employeeIds.isEmpty()) {
                return List.of();
            }
            return repository.findByEmployeeIdInAndCustomerIdOrderByCreatedAtDesc(employeeIds, customerId);
        }
    }

    @Override
    public CustomerActivity save(CustomerActivity activity) {
        return repository.save(activity);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private Pageable activityPageable(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
