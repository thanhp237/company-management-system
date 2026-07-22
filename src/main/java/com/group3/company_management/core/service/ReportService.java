package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.report.MonthlyRevenueDTO;
import com.group3.company_management.core.dto.report.SalesReportRowDTO;
import com.group3.company_management.core.dto.report.SalesSummaryDTO;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    SalesSummaryDTO getSalesSummary(LocalDate startDate, LocalDate endDate, Authentication authentication);

    List<MonthlyRevenueDTO> getMonthlyRevenueChartData(Authentication authentication);

    List<SalesReportRowDTO> getSalesReportRows(LocalDate startDate, LocalDate endDate, Long saleIdFilter, String statusFilter, Authentication authentication);

    byte[] exportSalesReportToExcel(LocalDate startDate, LocalDate endDate, Long saleIdFilter, String statusFilter, Authentication authentication);

    byte[] exportSalesReportToPdf(LocalDate startDate, LocalDate endDate, Long saleIdFilter, String statusFilter, Authentication authentication);
}
