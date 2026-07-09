package com.group3.company_management.core.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class PaymentScheduleResponse {
    private Long id;
    private Integer installmentNo;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String description;
    private Long invoiceId;
    private String invoiceStatus;
}
