package com.group3.company_management.core.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesTargetSummary {
    private Long targetId;
    private Long saleEmployeeId;
    private Long saleUserId;
    private String saleName;
    private String employeeCode;
    private Integer targetYear;
    private Integer targetMonth;
    private BigDecimal targetAmount;
    private BigDecimal achievedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal commissionAmount;
    private BigDecimal bonusRate;
    private BigDecimal bonusAmount;
    private BigDecimal achievementRate;
    private boolean targetReached;
    private String note;
}
