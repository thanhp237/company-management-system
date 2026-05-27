package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter
@Setter

public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    public Department() {

    }

    public Department(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
}
