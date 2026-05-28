package com.group3.company_management.core.dto;

import com.group3.company_management.core.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Long departmentId;
    private Long groupId;
    private Long roleId;
    private String roleName;
    private String status;

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setDepartmentId(user.getDepartmentId());
        response.setGroupId(user.getGroupId());
        if (user.getRole() != null) {
            response.setRoleId(user.getRole().getId());
            response.setRoleName(user.getRole().getRoleName());
        }
        response.setStatus(user.getStatus());
        return response;
    }
}
