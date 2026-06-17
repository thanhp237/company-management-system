package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.ProfileUpdateRequest;
import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.RoleRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersPage(String roleCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        boolean hasRole = roleCode != null && !roleCode.trim().isEmpty();

        Page<User> users = hasRole
                ? userRepository.findActiveUsersByRoleCode(roleCode, pageable)
                : userRepository.findAll(pageable);

        return users.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(findActiveUserById(id));
    }

    @Override
    public void createUser(UserRequest request) {
        String username = normalizeRequired(request.getUsername(), "Username is required");
        String email = normalizeRequired(request.getEmail(), "Email is required");
        String password = normalizeRequired(request.getPassword(), "Password is required");
        Role role = findRoleById(normalizeRequired(request.getRoleId(), "Role is required"));

        validateUniqueUsername(username, null);
        validateUniqueEmail(email, null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(normalizeOptional(request.getFullName()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setDepartmentId(request.getDepartmentId());
        user.setGroupId(request.getGroupId());
        user.setRole(role);
        user.setStatus("ACTIVE");
        userRepository.saveAndFlush(user);
        createRoleProfile(user, role);
    }

    @Override
    public void updateUser(UserRequest request) {
        Long id = normalizeRequired(request.getId(), "User ID is required");
        User user = findActiveUserById(id);
        String username = normalizeRequired(request.getUsername(), "Username is required");
        String email = normalizeRequired(request.getEmail(), "Email is required");
        Role role = findRoleById(normalizeRequired(request.getRoleId(), "Role is required"));

        validateUniqueUsername(username, id);
        validateUniqueEmail(email, id);

        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(normalizeOptional(request.getFullName()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setDepartmentId(request.getDepartmentId());
        user.setGroupId(request.getGroupId());
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User user = findActiveUserById(id);
        String username = normalizeRequired(userDetails.getUsername(), "Username is required");
        String email = normalizeRequired(userDetails.getEmail(), "Email is required");

        validateUniqueUsername(username, id);
        validateUniqueEmail(email, id);

        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(normalizeOptional(userDetails.getFullName()));
        user.setPhone(normalizeOptional(userDetails.getPhone()));
        user.setDepartmentId(userDetails.getDepartmentId());
        user.setGroupId(userDetails.getGroupId());
        user.setRole(userDetails.getRole());
        user.setStatus(normalizeOptional(userDetails.getStatus()));
        return userRepository.save(user);
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
            throw new IllegalArgumentException("User ID is required");
        }

        String status = normalizeRequired(request.getStatus(), "Status is required").toUpperCase(Locale.ROOT);
        User user = findActiveUserById(request.getId());
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    private User findActiveUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    private Role findRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));
    }

    private void createRoleProfile(User user, Role role) {
        String roleCode = normalizeRequired(role.getRoleCode(), "Role code is required").toUpperCase(Locale.ROOT);
        String employeeCode = buildEmployeeCode(roleCode, user.getId());

        switch (roleCode) {
            case "ADMIN" -> jdbcTemplate.update(
                    "INSERT INTO admins (account_id, employee_code) VALUES (?, ?)",
                    user.getId(), employeeCode
            );
            case "ADMIN_OFFICER" -> jdbcTemplate.update(
                    "INSERT INTO admin_officers (account_id, employee_code) VALUES (?, ?)",
                    user.getId(), employeeCode
            );
            case "ACCOUNTANT" -> jdbcTemplate.update(
                    "INSERT INTO accountants (account_id, employee_code) VALUES (?, ?)",
                    user.getId(), employeeCode
            );
            case "MARKETING" -> jdbcTemplate.update(
                    "INSERT INTO marketing_staffs (account_id, employee_code) VALUES (?, ?)",
                    user.getId(), employeeCode
            );
            case "SALES" -> jdbcTemplate.update(
                    "INSERT INTO sales_staffs (account_id, employee_code) VALUES (?, ?)",
                    user.getId(), employeeCode
            );
            default -> throw new IllegalArgumentException("Unsupported role code: " + roleCode);
        }
    }

    private String buildEmployeeCode(String roleCode, Long accountId) {
        String prefix = switch (roleCode) {
            case "ADMIN" -> "ADM";
            case "ADMIN_OFFICER" -> "AOF";
            case "ACCOUNTANT" -> "ACC";
            case "MARKETING" -> "MKT";
            case "SALES" -> "SAL";
            default -> throw new IllegalArgumentException("Unsupported role code: " + roleCode);
        };

        return "%s%06d".formatted(prefix, accountId);
    }

    private void validateUniqueUsername(String username, Long currentUserId) {
        boolean exists;
        if (currentUserId == null) {
            exists = userRepository.existsByUsernameAndIsDeletedFalse(username);
        } else {
            exists = userRepository.existsByUsernameAndIsDeletedFalseAndIdNot(username, currentUserId);
        }

        if (exists) {
            throw new IllegalArgumentException("Username already exists");
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
            throw new IllegalArgumentException("Email already exists");
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
    
    return userRepository.findActiveUsersByRoleCode(roleCode)
            .stream()
            .map(UserResponse::fromEntity) //  hàm có sẵn
            .toList();
}

@Override
@Transactional(readOnly = true)
public UserResponse getProfileByUsername(String username) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    return UserResponse.fromEntity(user);
}

@Override
@Transactional
public void updateProfile(String username, ProfileUpdateRequest request) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
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
                    .findByUsernameContainingIgnoreCaseAndStatusIgnoreCaseOrEmailContainingIgnoreCaseAndStatusIgnoreCase(
                            keyword, status, keyword, status
                    );

        } else if (hasKeyword) {
            users = userRepository
                    .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            keyword, keyword
                    );

        } else {
            users = userRepository
                    .findByStatus(status);
        }

        return users.stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchPage(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        return userRepository.searchUsers(keyword, status, pageable)
                .map(UserResponse::fromEntity);
    }

    @Override
    @Transactional
    public Long countUsers (){
        return userRepository.count();
    }
}
