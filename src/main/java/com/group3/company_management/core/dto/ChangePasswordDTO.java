package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDTO {

    private String currentPassword;

    private String newPassword;

    private String confirmPassword;
}