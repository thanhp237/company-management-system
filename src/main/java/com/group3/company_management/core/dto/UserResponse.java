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
    private String departmentName;
    private Long roleId;
    private String roleName;
    private String status;
    private String employeeCode;

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setDepartmentId(user.getDepartmentId());
        if (user.getRole() != null) {
            response.setRoleId(user.getRole().getId());
            response.setRoleName(user.getRole().getRoleName());
        }
        if (user.getEmployee() != null) {
            response.setEmployeeCode(user.getEmployee().getEmployeeCode());
        } else {
            response.setEmployeeCode(user.getEmployeeCode());
        }
        response.setStatus(user.getStatus());
        return response;
    }
}
