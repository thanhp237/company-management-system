package com.group3.company_management.core.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDTO {
    private BigDecimal totalSignedRevenue;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalOutstanding;
    private BigDecimal averageOrderValue;
    private long totalContractsCount;
    private long totalInvoicesCount;
    private long approvedContractsCount;
    private BigDecimal approvedContractsAmount;
}
