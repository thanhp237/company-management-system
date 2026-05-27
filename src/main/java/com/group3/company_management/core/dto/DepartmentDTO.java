package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class DepartmentDTO {
private String code;
private String name;
private Long id;
    public DepartmentDTO() {
    }

    public DepartmentDTO(Long id,String code, String name) {
        this.code = code;
        this.name = name;
        this.id= id;
    }
}
