package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.report.MonthlyRevenueDTO;
import com.group3.company_management.core.dto.report.SalesReportRowDTO;
import com.group3.company_management.core.dto.report.SalesSummaryDTO;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.ReportService;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Override
    public SalesSummaryDTO getSalesSummary(LocalDate startDate, LocalDate endDate, Long saleIdFilter, Authentication authentication) {
        LocalDateTime startDT = startDate.atStartOfDay();
        LocalDateTime endDT = endDate.atTime(LocalTime.MAX);

        List<Long> targetSaleIds = resolveTargetSaleIds(saleIdFilter, authentication);

        BigDecimal signedRevenue;
        List<Contract> contracts;
        BigDecimal paidAmount;
        BigDecimal outstandingAmount;

        if (targetSaleIds != null) {
            if (targetSaleIds.isEmpty()) {
                signedRevenue = BigDecimal.ZERO;
                contracts = List.of();
                paidAmount = BigDecimal.ZERO;
                outstandingAmount = BigDecimal.ZERO;
            } else {
                signedRevenue = contractRepository.sumFinalAmountBySaleIdInAndStatusAndCreatedAtBetween(targetSaleIds, Contract.ContractStatus.SIGNED, startDT, endDT);
                contracts = contractRepository.findBySaleIdInAndCreatedAtBetweenOrderByCreatedAtDesc(targetSaleIds, startDT, endDT);
                paidAmount = invoiceRepository.sumPaidAmountBySaleIdInBetween(targetSaleIds, startDT, endDT);
                outstandingAmount = invoiceRepository.sumOutstandingAmountBySaleIdInBetween(targetSaleIds, startDT, endDT);
            }
        } else {
            signedRevenue = contractRepository.sumFinalAmountByStatusAndCreatedAtBetween(Contract.ContractStatus.SIGNED, startDT, endDT);
            contracts = contractRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDT, endDT);
            paidAmount = invoiceRepository.sumPaidAmountBetween(startDT, endDT);
            outstandingAmount = invoiceRepository.sumOutstandingAmountBetween(startDT, endDT);
        }

        long signedContractsCount = contracts.stream().filter(c -> c.getStatus() == Contract.ContractStatus.SIGNED).count();

        long approvedContractsCount = contracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.ADMIN_REVIEWED || c.getStatus() == Contract.ContractStatus.SENT_TO_CUSTOMER || c.getStatus() == Contract.ContractStatus.SIGNED)
                .count();

        BigDecimal approvedContractsAmount = contracts.stream()
                .filter(c -> (c.getStatus() == Contract.ContractStatus.ADMIN_REVIEWED || c.getStatus() == Contract.ContractStatus.SENT_TO_CUSTOMER || c.getStatus() == Contract.ContractStatus.SIGNED) && c.getFinalAmount() != null)
                .map(Contract::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal aov = BigDecimal.ZERO;
        if (signedContractsCount > 0) {
            aov = (signedRevenue != null ? signedRevenue : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(signedContractsCount), 0, RoundingMode.HALF_UP);
        }

        return SalesSummaryDTO.builder()
                .totalSignedRevenue(signedRevenue != null ? signedRevenue : BigDecimal.ZERO)
                .totalPaidAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO)
                .totalOutstanding(outstandingAmount != null ? outstandingAmount : BigDecimal.ZERO)
                .averageOrderValue(aov)
                .totalContractsCount(contracts.size())
                .totalInvoicesCount(invoiceRepository.count())
                .approvedContractsCount(approvedContractsCount)
                .approvedContractsAmount(approvedContractsAmount)
                .build();
    }

    @Override
    public List<MonthlyRevenueDTO> getMonthlyRevenueChartData(Long saleIdFilter, Authentication authentication) {
        List<MonthlyRevenueDTO> chartData = new ArrayList<>();
        LocalDate now = LocalDate.now();
        List<Long> targetSaleIds = resolveTargetSaleIds(saleIdFilter, authentication);

        // 6 months timeline data calculation
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            LocalDate start = monthDate.withDayOfMonth(1);
            LocalDate end = monthDate.withDayOfMonth(monthDate.lengthOfMonth());

            LocalDateTime startDT = start.atStartOfDay();
            LocalDateTime endDT = end.atTime(LocalTime.MAX);

            BigDecimal signed;
            BigDecimal paid;

            if (targetSaleIds != null) {
                if (targetSaleIds.isEmpty()) {
                    signed = BigDecimal.ZERO;
                    paid = BigDecimal.ZERO;
                } else {
                    signed = contractRepository.sumFinalAmountBySaleIdInAndStatusAndCreatedAtBetween(targetSaleIds, Contract.ContractStatus.SIGNED, startDT, endDT);
                    paid = invoiceRepository.sumPaidAmountBySaleIdInBetween(targetSaleIds, startDT, endDT);
                }
            } else {
                signed = contractRepository.sumFinalAmountByStatusAndCreatedAtBetween(Contract.ContractStatus.SIGNED, startDT, endDT);
                paid = invoiceRepository.sumPaidAmountBetween(startDT, endDT);
            }

            String label = "Tháng " + monthDate.getMonthValue() + "/" + monthDate.getYear();
            chartData.add(MonthlyRevenueDTO.builder()
                    .monthLabel(label)
                    .signedRevenue(signed != null ? signed : BigDecimal.ZERO)
                    .paidRevenue(paid != null ? paid : BigDecimal.ZERO)
                    .build());
        }
        return chartData;
    }

    @Override
    public List<SalesReportRowDTO> getSalesReportRows(LocalDate startDate, LocalDate endDate, Long saleIdFilter, String statusFilter, Authentication authentication) {
        LocalDateTime startDT = startDate.atStartOfDay();
        LocalDateTime endDT = endDate.atTime(LocalTime.MAX);

        List<Long> allowedSaleIds = resolveAllowedSaleIds(authentication);
        List<Contract> contracts;

        if (allowedSaleIds != null) {
            if (allowedSaleIds.isEmpty()) {
                contracts = List.of();
            } else {
                contracts = contractRepository.findBySaleIdInAndCreatedAtBetweenOrderByCreatedAtDesc(allowedSaleIds, startDT, endDT);
            }
        } else {
            contracts = contractRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDT, endDT);
        }

        List<SalesReportRowDTO> rows = new ArrayList<>();

        for (Contract c : contracts) {
            // Optional Filter by saleIdFilter
            if (saleIdFilter != null && (c.getSale() == null || !c.getSale().getId().equals(saleIdFilter))) {
                continue;
            }
            // Optional Filter by statusFilter
            if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
                if (!c.getStatus().name().equalsIgnoreCase(statusFilter)) {
                    continue;
                }
            }

            BigDecimal totalAmount = c.getFinalAmount() != null ? c.getFinalAmount() : BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;
            BigDecimal outstandingAmount = totalAmount;

            String saleName = "Chưa gán Sale";
            try {
                if (c.getSale() != null && c.getSale().getUser() != null) {
                    saleName = c.getSale().getUser().getFullName();
                }
            } catch (Exception ex) {
                saleName = c.getSale() != null ? "Sale #" + c.getSale().getId() : "Chưa gán Sale";
            }

            rows.add(SalesReportRowDTO.builder()
                    .contractId(c.getId())
                    .contractCode(c.getContractCode())
                    .customerName(c.getCustomer() != null ? c.getCustomer().getCompanyName() : "Khách hàng cá nhân/N/A")
                    .saleName(saleName)
                    .signedDate(c.getSignedAt() != null ? c.getSignedAt().toLocalDate() : c.getCreatedAt().toLocalDate())
                    .finalAmount(totalAmount)
                    .paidAmount(paidAmount)
                    .outstandingAmount(outstandingAmount)
                    .status(c.getStatus() != null ? c.getStatus().name() : "DRAFT")
                    .build());
        }

        return rows;
    }

    @Override
    public byte[] exportSalesReportToExcel(LocalDate startDate, LocalDate endDate, Long saleIdFilter, String statusFilter, Authentication authentication) {
        List<SalesReportRowDTO> rows = getSalesReportRows(startDate, endDate, saleIdFilter, statusFilter, authentication);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao_Cao_Doanh_So");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Currency Style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0\" ₫\""));

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "Mã Hợp Đồng", "Tên Khách Hàng", "Nhân Viên Sales", "Ngày Ký", "Tổng Giá Trị (₫)", "Đã Thu (₫)", "Còn Phải Thu (₫)", "Trạng Thái"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal sumTotal = BigDecimal.ZERO;
            BigDecimal sumPaid = BigDecimal.ZERO;
            BigDecimal sumDebt = BigDecimal.ZERO;

            for (SalesReportRowDTO dto : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(dto.getContractCode() != null ? dto.getContractCode() : "");
                row.createCell(2).setCellValue(dto.getCustomerName() != null ? dto.getCustomerName() : "");
                row.createCell(3).setCellValue(dto.getSaleName() != null ? dto.getSaleName() : "");
                row.createCell(4).setCellValue(dto.getSignedDate() != null ? dto.getSignedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");

                Cell cTotal = row.createCell(5);
                cTotal.setCellValue(dto.getFinalAmount().doubleValue());
                cTotal.setCellStyle(currencyStyle);

                Cell cPaid = row.createCell(6);
                cPaid.setCellValue(dto.getPaidAmount().doubleValue());
                cPaid.setCellStyle(currencyStyle);

                Cell cDebt = row.createCell(7);
                cDebt.setCellValue(dto.getOutstandingAmount().doubleValue());
                cDebt.setCellStyle(currencyStyle);

                row.createCell(8).setCellValue(dto.getStatus());

                sumTotal = sumTotal.add(dto.getFinalAmount());
                sumPaid = sumPaid.add(dto.getPaidAmount());
                sumDebt = sumDebt.add(dto.getOutstandingAmount());
            }

            // Summary Row
            Row summaryRow = sheet.createRow(rowIdx);
            CellStyle summaryStyle = workbook.createCellStyle();
            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryStyle.setFont(summaryFont);

            Cell sumLabel = summaryRow.createCell(0);
            sumLabel.setCellValue("TỔNG CỘNG");
            sumLabel.setCellStyle(summaryStyle);

            Cell sumTotalCell = summaryRow.createCell(5);
            sumTotalCell.setCellValue(sumTotal.doubleValue());
            sumTotalCell.setCellStyle(currencyStyle);

            Cell sumPaidCell = summaryRow.createCell(6);
            sumPaidCell.setCellValue(sumPaid.doubleValue());
            sumPaidCell.setCellStyle(currencyStyle);

            Cell sumDebtCell = summaryRow.createCell(7);
            sumDebtCell.setCellValue(sumDebt.doubleValue());
            sumDebtCell.setCellStyle(currencyStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi xuất file Excel báo cáo doanh số", e);
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportSalesReportToPdf(LocalDate startDate, LocalDate endDate, Long saleIdFilter, String statusFilter, Authentication authentication) {
        List<SalesReportRowDTO> rows = getSalesReportRows(startDate, endDate, saleIdFilter, statusFilter, authentication);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("BAO CAO DOANH SO KINH DOANH", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph period = new Paragraph("Tu ngay: " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                    + " den ngay: " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), FontFactory.getFont(FontFactory.HELVETICA, 11));
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(20);
            document.add(period);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3.5f, 2.5f, 2f, 2.5f, 2.5f, 2f});

            String[] headers = {"Ma HD", "Khach Hang", "Sale", "Ngay Ky", "Gia Tri", "Da Thu", "Trang Thai"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            for (SalesReportRowDTO r : rows) {
                table.addCell(new Phrase(r.getContractCode(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(r.getCustomerName(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(r.getSaleName(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(r.getSignedDate() != null ? r.getSignedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "", FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(currencyFormat.format(r.getFinalAmount()), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(currencyFormat.format(r.getPaidAmount()), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(r.getStatus(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi xuất file PDF báo cáo doanh số", e);
            throw new RuntimeException("Lỗi tạo file PDF: " + e.getMessage());
        }
    }

    /**
     * Data Scope resolution helper:
     * - ROLE_SALES -> Only see their own contract records.
     * - ROLE_SALES_MANAGER / ROLE_MANAGER -> See all sales employees in their department.
     * - ROLE_DIRECTOR / ROLE_ACCOUNTANT / ROLE_ADMIN / ROLE_ADMIN_OFFICER -> See all company records (returns null).
     */
    private List<Long> resolveAllowedSaleIds(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return List.of();
        }

        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        Employee currentEmployee = employeeRepository.findByUser_Username(username).orElse(null);

        if ("SALES".equals(role)) {
            if (currentEmployee == null || currentEmployee.getId() == null) {
                return List.of();
            }
            return List.of(currentEmployee.getId());
        }

        if ("SALES_MANAGER".equals(role) || "MANAGER".equals(role)) {
            if (currentEmployee == null || currentEmployee.getUser() == null
                    || currentEmployee.getUser().getDepartmentId() == null) {
                return List.of();
            }
            Long deptId = currentEmployee.getUser().getDepartmentId();
            return userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(deptId)
                    .stream()
                    .map(User::getEmployee)
                    .filter(e -> e != null && e.getId() != null)
                    .map(Employee::getId)
                    .toList();
        }

        // For DIRECTOR, ACCOUNTANT, ADMIN, ADMIN_OFFICER, MARKETING -> Return null (All company data)
        return null;
    }

    private List<Long> resolveTargetSaleIds(Long saleIdFilter, Authentication authentication) {
        List<Long> allowedSaleIds = resolveAllowedSaleIds(authentication);
        if (saleIdFilter != null) {
            if (allowedSaleIds != null && !allowedSaleIds.contains(saleIdFilter)) {
                return List.of();
            }
            return List.of(saleIdFilter);
        }
        return allowedSaleIds;
    }

    @Override
    public List<com.group3.company_management.core.dto.report.SalesReportSaleOptionDTO> getSalesOptionsForUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return List.of();
        }

        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        Employee currentEmployee = employeeRepository.findByUser_Username(username).orElse(null);
        List<Employee> eligibleEmployees = new ArrayList<>();

        if ("ADMIN".equals(role) || "DIRECTOR".equals(role)) {
            eligibleEmployees = employeeRepository.findAll();
        } else if ("SALES_MANAGER".equals(role) || "MANAGER".equals(role)) {
            if (currentEmployee != null && currentEmployee.getUser() != null && currentEmployee.getUser().getDepartmentId() != null) {
                Long deptId = currentEmployee.getUser().getDepartmentId();
                List<User> usersInDept = userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(deptId);
                eligibleEmployees = usersInDept.stream()
                        .map(User::getEmployee)
                        .filter(e -> e != null && e.getId() != null)
                        .toList();
            }
        } else if ("SALES".equals(role)) {
            if (currentEmployee != null) {
                eligibleEmployees = List.of(currentEmployee);
            }
        } else {
            eligibleEmployees = employeeRepository.findAll();
        }

        return eligibleEmployees.stream()
                .filter(e -> {
                    try {
                        if (e.getUser() != null && e.getUser().getRole() != null) {
                            String roleCode = e.getUser().getRole().getRoleCode();
                            return !"ADMIN".equalsIgnoreCase(roleCode)
                                    && !"DIRECTOR".equalsIgnoreCase(roleCode)
                                    && !"SALES_MANAGER".equalsIgnoreCase(roleCode)
                                    && !"MANAGER".equalsIgnoreCase(roleCode)
                                    && !"ADMIN_OFFICER".equalsIgnoreCase(roleCode)
                                    && !"ADMINOFFICER".equalsIgnoreCase(roleCode);
                        }
                    } catch (Exception ex) {
                        return true;
                    }
                    return true;
                })
                .map(e -> {
                    String name = "Sale #" + e.getId();
                    try {
                        if (e.getUser() != null) {
                            name = e.getUser().getFullName();
                        }
                    } catch (Exception ex) {
                        name = "Sale #" + (e.getEmployeeCode() != null ? e.getEmployeeCode() : e.getId());
                    }
                    return com.group3.company_management.core.dto.report.SalesReportSaleOptionDTO.builder()
                            .saleId(e.getId())
                            .fullName(name)
                            .employeeCode(e.getEmployeeCode() != null ? e.getEmployeeCode() : "")
                            .build();
                })
                .toList();
    }
}
