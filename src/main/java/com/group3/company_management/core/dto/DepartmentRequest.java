package com.group3.company_management.core.dto;

import com.group3.company_management.core.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
public class DepartmentRequest {
    private Long id;

    private String code;

    private String name;

    private String description;

    private Long managerId;

    private Integer maxMembers;
    private String status ;
    private String managerName;
    private List<User> employees;
}
