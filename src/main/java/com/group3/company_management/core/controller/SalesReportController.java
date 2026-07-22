package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.report.MonthlyRevenueDTO;
import com.group3.company_management.core.dto.report.SalesReportRowDTO;
import com.group3.company_management.core.dto.report.SalesSummaryDTO;
import com.group3.company_management.core.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports/sales")
@PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_OFFICER', 'ADMINOFFICER', 'SALES', 'MARKETING', 'SALES_MANAGER', 'MANAGER', 'ACCOUNTANT', 'DIRECTOR')")
@RequiredArgsConstructor
@Slf4j
public class SalesReportController {

    private final ReportService reportService;

    @GetMapping
    public String salesReportPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long saleId,
            @RequestParam(required = false) String status,
            Authentication authentication,
            Model model) {

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        SalesSummaryDTO summary = reportService.getSalesSummary(startDate, endDate, authentication);
        List<MonthlyRevenueDTO> chartData = reportService.getMonthlyRevenueChartData(authentication);
        List<SalesReportRowDTO> rows = reportService.getSalesReportRows(startDate, endDate, saleId, status, authentication);

        List<String> chartLabels = chartData.stream().map(MonthlyRevenueDTO::getMonthLabel).toList();
        List<BigDecimal> chartSigned = chartData.stream().map(MonthlyRevenueDTO::getSignedRevenue).toList();
        List<BigDecimal> chartPaid = chartData.stream().map(MonthlyRevenueDTO::getPaidRevenue).toList();

        model.addAttribute("title", "Báo Cáo & Thống Kê Doanh Số");
        model.addAttribute("summary", summary);
        model.addAttribute("chartData", chartData);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartSigned", chartSigned);
        model.addAttribute("chartPaid", chartPaid);
        model.addAttribute("reportRows", rows);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedStatus", status != null ? status : "ALL");

        return "reports/sales-report";
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long saleId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        byte[] excelData = reportService.exportSalesReportToExcel(startDate, endDate, saleId, status, authentication);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_Doanh_So_" + startDate + "_to_" + endDate + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long saleId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        byte[] pdfData = reportService.exportSalesReportToPdf(startDate, endDate, saleId, status, authentication);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_Doanh_So_" + startDate + "_to_" + endDate + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
