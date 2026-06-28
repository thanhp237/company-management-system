package com.group3.company_management.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerAccountResult {

    private boolean emailSent;

    private String username;

    private String rawPassword;
}
