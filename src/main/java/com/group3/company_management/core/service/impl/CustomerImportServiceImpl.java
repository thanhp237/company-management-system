package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.LeadDTO;
import com.group3.company_management.core.repository.LeadRepository;
import com.group3.company_management.core.service.CustomerImportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerImportServiceImpl implements CustomerImportService {
 private LeadRepository leadRepository;

    public CustomerImportServiceImpl(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Override
    public List<LeadDTO> importCustomer(MultipartFile file) {

        validateExcelFile(file);

        List<LeadDTO> leads = new ArrayList<>();

        try (Workbook workbook =
                     new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                LeadDTO dto = new LeadDTO();

                dto.setFullName(getCellValue(row.getCell(0)));
                dto.setPhone(getCellValue(row.getCell(1)));
                dto.setEmail(getCellValue(row.getCell(2)));
                dto.setAddress(getCellValue(row.getCell(3)));
                dto.setCompanyName(getCellValue(row.getCell(4)));
                dto.setTaxCode(getCellValue(row.getCell(5)));
                dto.setCustomerSource(getCellValue(row.getCell(6)));
                dto.setCustomerType(getCellValue(row.getCell(7)));

                leads.add(dto);
            }

        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage());
        }

        return leads;
    }

    private void validateExcelFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty!");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null ||
                (!fileName.endsWith(".xlsx")
                        && !fileName.endsWith(".xls"))) {

            throw new RuntimeException(
                    "Only Excel files (.xlsx, .xls) are allowed!"
            );
        }
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }
}