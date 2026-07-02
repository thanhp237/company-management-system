package com.group3.company_management.core.dto;

import com.group3.company_management.core.entity.Department;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class DepartmentResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    private Long managerId;
    private String managerName;
    private Integer maxMembers;
    private String status ;
    private Integer currentMembers;
    public static DepartmentResponse fromEntity(Department department,String name,int currentMembers) {

        DepartmentResponse response = new DepartmentResponse();

        response.setId(department.getId());
        response.setCode(department.getCode());
        response.setName(department.getName());
        response.setDescription(department.getDescription());
        response.setManagerId(department.getManagerId());
        response.setMaxMembers(department.getMaxMembers());
        response.setStatus(department.getStatus());
        response.setManagerName(name);
        response.setCurrentMembers(currentMembers);
        return response;
    }
}
