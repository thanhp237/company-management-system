package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.ProfileUpdateRequest;
import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.entity.User;

import java.util.List;

import org.springframework.data.domain.Page;

public interface UserService {
    List<UserResponse> getAllUsers();

    Page<UserResponse> getUsersPage(String roleCode, int page, int size);

    UserResponse getUserById(Long id);

    void createUser(UserRequest request);

    void updateUser(UserRequest request);

    User updateUser(Long id, User userDetails);

    void deleteUser(Long id);

    void updateUserStatus(UserRequest request);

    List<Role> getAllRoles();

    //
    List<UserResponse> getActiveUsersByRole(String roleCode);

    UserResponse getProfileByUsername(String username);

    void updateProfile(String username, ProfileUpdateRequest request);

    List<UserResponse> search(String keyword, String status);

    Page<UserResponse> searchPage(String keyword, String status, String roleCode, int page, int size);

    Long countUsers();
    List<User> getUsersByRoles(List<String> roles);
}
