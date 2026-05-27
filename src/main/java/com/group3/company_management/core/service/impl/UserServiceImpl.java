package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.UserService;
import lombok.RequiredArgsConstructor;
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
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(findActiveUserById(id));
    }

    @Override
    public void createUser(UserRequest request) {
        String username = normalizeRequired(request.getUsername(), "Username is required");
        String email = normalizeRequired(request.getEmail(), "Email is required");
        String password = normalizeRequired(request.getPassword(), "Password is required");

        validateUniqueUsername(username, null);
        validateUniqueEmail(email, null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");
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
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
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

    private User findActiveUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
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
}
