package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.SalesTargetSummary;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface SalesTargetService {
    List<SalesTargetSummary> getTargetSummaries(String username, YearMonth period);

    List<SalesTargetSummary> getMyTargetSummaries(String username, YearMonth period);

    void saveTarget(String username,
                    Long saleEmployeeId,
                    Integer targetYear,
                    Integer targetMonth,
                    BigDecimal targetAmount,
                    BigDecimal bonusRate,
                    String note);
}
