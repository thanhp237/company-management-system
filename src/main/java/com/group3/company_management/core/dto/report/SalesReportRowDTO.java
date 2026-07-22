package com.group3.company_management.core.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportRowDTO {
    private Long contractId;
    private String contractCode;
    private String customerName;
    private String saleName;
    private LocalDate signedDate;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String status;
}
