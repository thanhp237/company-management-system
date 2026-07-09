package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class PaymentScheduleRequest {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    private BigDecimal amount;
    private String description;
}
