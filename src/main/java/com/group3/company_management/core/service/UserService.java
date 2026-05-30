package com.group3.company_management.core.service;

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
    // Thêm vào trong interface UserService hiện tại của bạn:
List<UserResponse> getActiveUsersByRole(String roleCode);
}
