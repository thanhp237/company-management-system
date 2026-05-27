package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String status;
}
