package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class ContractRuleRequest {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractEndDate;

    private String paymentTerms;

    private String deliveryTerms;

    private String legalTerms;

    private String adminNote;
}
