package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.ProfileUpdateRequest;
import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.DepartmentRepository;
import com.group3.company_management.core.repository.RoleRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return toResponseList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersPage(String roleCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        boolean hasRole = roleCode != null && !roleCode.trim().isEmpty();
        Page<User> users;
        if (hasRole){
            users = userRepository.findActiveUsersByRoleCode(roleCode, pageable);
        }else {
            users = userRepository.findAllActiveWithEmployee(pageable);
        }
        return toResponsePage(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findActiveUserById(id), Map.of());
    }

    @Override
    public void createUser(UserRequest request) {
        String username = normalizeRequired(request.getUsername(), "Vui lòng nhập tên đăng nhập");
        String email = normalizeRequired(request.getEmail(), "Vui lòng nhập email");
        String password = normalizeRequired(request.getPassword(), "Vui lòng nhập mật khẩu");
        Role role = findRoleById(normalizeRequired(request.getRoleId(), "Vui lòng chọn vai trò"));

        validateUniqueUsername(username, null);
        validateUniqueEmail(email, null);
        validateDepartment(request.getDepartmentId());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(normalizeOptional(request.getFullName()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setDepartmentId(request.getDepartmentId());
        user.setRole(role);
        user.setStatus("ACTIVE");
        userRepository.saveAndFlush(user);
        syncEmployeeProfile(user, role);
    }

    @Override
    public void updateUser(UserRequest request) {
        Long id = normalizeRequired(request.getId(), "Thiếu mã tài khoản");
        User user = findActiveUserById(id);
        String username = normalizeRequired(request.getUsername(), "Vui lòng nhập tên đăng nhập");
        String email = normalizeRequired(request.getEmail(), "Vui lòng nhập email");
        Role role = findRoleById(normalizeRequired(request.getRoleId(), "Vui lòng chọn vai trò"));

        validateUniqueUsername(username, id);
        validateUniqueEmail(email, id);
        validateDepartment(request.getDepartmentId());

        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(normalizeOptional(request.getFullName()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setDepartmentId(request.getDepartmentId());
        user.setRole(role);
        userRepository.saveAndFlush(user);
        syncEmployeeProfile(user, role);
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User user = findActiveUserById(id);
        String username = normalizeRequired(userDetails.getUsername(), "Vui lòng nhập tên đăng nhập");
        String email = normalizeRequired(userDetails.getEmail(), "Vui lòng nhập email");

        validateUniqueUsername(username, id);
        validateUniqueEmail(email, id);
        validateDepartment(userDetails.getDepartmentId());

        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(normalizeOptional(userDetails.getFullName()));
        user.setPhone(normalizeOptional(userDetails.getPhone()));
        user.setDepartmentId(userDetails.getDepartmentId());
        user.setRole(userDetails.getRole());
        user.setStatus(normalizeOptional(userDetails.getStatus()));
        User savedUser = userRepository.saveAndFlush(user);
        if (savedUser.getRole() != null) {
            syncEmployeeProfile(savedUser, savedUser.getRole());
        }
        return savedUser;
    }

    @Override
    public void deleteUser(Long id) {
        User user = findActiveUserById(id);
        user.setDeleted(true);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void updateUserStatus(UserRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("Thiếu mã tài khoản");
        }

        String status = normalizeRequired(request.getStatus(), "Vui lòng chọn trạng thái").toUpperCase(Locale.ROOT);
        User user = findActiveUserById(request.getId());
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findByStatusIgnoreCaseOrderByRoleNameAsc("ACTIVE");
    }
    @Override
    public List<User> getUsersByRoles(List<String> roles) {
        List<User> user =userRepository.findUsersByRoleNames(roles);

        return userRepository.findUsersByRoleNames(roles);
    }
    private User findActiveUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản có mã: " + id));
    }

    private Role findRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò có mã: " + id));
        if (!role.isActive()) {
            throw new IllegalArgumentException("Vai trò đang ngừng hoạt động");
        }
        return role;
    }

    private void syncEmployeeProfile(User user, Role role) {
        String roleCode = normalizeRequired(role.getRoleCode(), "Vui lòng nhập mã vai trò").toUpperCase(Locale.ROOT);
        String employeeCode = buildEmployeeCode(roleCode, user.getId());

        validateEmployeeRole(roleCode);
        jdbcTemplate.update(
                """
                        INSERT INTO employees (account_id, employee_code, employee_type)
                        VALUES (?, ?, ?)
                        ON CONFLICT (account_id)
                        DO UPDATE SET employee_code = EXCLUDED.employee_code,
                                      employee_type = EXCLUDED.employee_type
                        """,
                user.getId(), employeeCode, roleCode
        );
    }

    private String buildEmployeeCode(String roleCode, Long accountId) {
        String prefix = switch (roleCode) {
            case "ADMIN" -> "ADM";
            case "ADMIN_OFFICER" -> "AOF";
            case "ACCOUNTANT" -> "ACC";
            case "DIRECTOR" -> "DIR";
            case "MANAGER" -> "MGR";
            case "MARKETING" -> "MKT";
            case "SALES" -> "SAL";
            case "SALES_MANAGER" -> "SLM";
            default -> throw new IllegalArgumentException("Mã vai trò không được hỗ trợ: " + roleCode);
        };

        return "%s%06d".formatted(prefix, accountId);
    }

    private void validateEmployeeRole(String roleCode) {
        switch (roleCode) {
            case "ADMIN", "ADMIN_OFFICER", "ACCOUNTANT", "DIRECTOR",
                    "MANAGER", "MARKETING", "SALES", "SALES_MANAGER" -> {
            }
            default -> throw new IllegalArgumentException("Mã vai trò không được hỗ trợ: " + roleCode);
        }
    }

    private void validateUniqueUsername(String username, Long currentUserId) {
        boolean exists;
        if (currentUserId == null) {
            exists = userRepository.existsByUsernameAndIsDeletedFalse(username);
        } else {
            exists = userRepository.existsByUsernameAndIsDeletedFalseAndIdNot(username, currentUserId);
        }

        if (exists) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
    }

    private void validateUniqueEmail(String email, Long currentUserId) {
        boolean exists;
        if (currentUserId == null) {
            exists = userRepository.existsByEmailAndIsDeletedFalse(email);
        } else {
            exists = userRepository.existsByEmailAndIsDeletedFalseAndIdNot(email, currentUserId);
        }

        if (exists) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private Long normalizeRequired(Long value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

@Override
@Transactional(readOnly = true)
    public List<UserResponse> getActiveUsersByRole(String roleCode) {
    if (roleCode == null || roleCode.trim().isEmpty()) {
        return getAllUsers();
    }
    
    return toResponseList(userRepository.findActiveUsersByRoleCode(roleCode));
}

@Override
@Transactional(readOnly = true)
public UserResponse getProfileByUsername(String username) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    return toResponse(user, Map.of());
}

@Override
@Transactional
public void updateProfile(String username, ProfileUpdateRequest request) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    
    // cho update
    user.setFullName(request.getFullName());
    user.setEmail(request.getEmail());
    user.setPhone(request.getPhone());
    
    userRepository.save(user);
}
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> search(String keyword, String status) {

        List<User> users;

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (!hasKeyword && !hasStatus) {
            users = userRepository.findAll();

        } else if (hasKeyword && hasStatus) {
            users = userRepository
                    .searchByKeywordAndStatus(keyword, status);

        } else if (hasKeyword) {
            users = userRepository
                    .searchByKeyword(keyword);

        } else {
            users = userRepository
                    .searchByStatus(status);
        }

        return toResponseList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchPage(String keyword, String status, String roleCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("id").ascending());
        return toResponsePage(userRepository.searchUsers(keyword, status, roleCode, pageable));
    }

    @Override
    @Transactional
    public Long countUsers (){
        return userRepository.count();
    }

    private Page<UserResponse> toResponsePage(Page<User> users) {
        Map<Long, String> departmentNames = loadDepartmentNames(users.getContent());
        return users.map(user -> toResponse(user, departmentNames));
    }

    private List<UserResponse> toResponseList(List<User> users) {
        Map<Long, String> departmentNames = loadDepartmentNames(users);
        return users.stream()
                .map(user -> toResponse(user, departmentNames))
                .toList();
    }

    private UserResponse toResponse(User user, Map<Long, String> departmentNames) {
        UserResponse response = UserResponse.fromEntity(user);
        if (response.getDepartmentId() != null) {
            response.setDepartmentName(departmentNames.get(response.getDepartmentId()));
        }
        return response;
    }

    private Map<Long, String> loadDepartmentNames(Collection<User> users) {
        List<Long> departmentIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (departmentIds.isEmpty()) {
            return Map.of();
        }

        return departmentRepository.findByIdInAndIsDeletedFalse(departmentIds)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    private void validateDepartment(Long departmentId) {
        if (departmentId == null) {
            return;
        }

        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ban"));

        if (!"ACTIVE".equalsIgnoreCase(department.getStatus())) {
            throw new IllegalArgumentException("Phòng ban đang ngừng hoạt động");
        }
    }
}
