package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.ProfileUpdateRequest;
import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.entity.User;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
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
}
