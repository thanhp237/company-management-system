package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.LeadDTO;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.LeadRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.RoleRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.CustomerImportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerImportServiceImpl implements CustomerImportService {
    private LeadRepository leadRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private OpportunityRepository opportunityRepository;

    public CustomerImportServiceImpl(RoleRepository roleRepository,
                                     UserRepository userRepository,
                                     LeadRepository leadRepository,
                                     OpportunityRepository opportunityRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
    }

    @Override
    public void importCustomer(MultipartFile file, String name) {

        validateExcelFile(file);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);

            if (header == null ||
                    !"Full Name".equalsIgnoreCase(getCellValue(header.getCell(0))) ||
                    !"Gender".equalsIgnoreCase(getCellValue(header.getCell(1))) ||
                    !"Phone".equalsIgnoreCase(getCellValue(header.getCell(2))) ||
                    !"Email".equalsIgnoreCase(getCellValue(header.getCell(3))) ||
                    !"Address".equalsIgnoreCase(getCellValue(header.getCell(4))) ||
                    !"Company Name".equalsIgnoreCase(getCellValue(header.getCell(5))) ||
                    !"Tax Code".equalsIgnoreCase(getCellValue(header.getCell(6))) ||
                    !"Customer Source".equalsIgnoreCase(getCellValue(header.getCell(7))) ||
                    !"Customer Type".equalsIgnoreCase(getCellValue(header.getCell(8)))) {

                throw new RuntimeException(
                        "Invalid Excel template. Please download and use the provided template."
                );
            }

            User creator = userRepository.findByUsername(name)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                String fullName = getCellValue(row.getCell(0));
                String gender = getCellValue(row.getCell(1));
                String phone = getCellValue(row.getCell(2));
                String email = getCellValue(row.getCell(3));
                String address = getCellValue(row.getCell(4));
                String companyName = getCellValue(row.getCell(5));
                String taxCode = getCellValue(row.getCell(6));
                String customerSource = getCellValue(row.getCell(7));
                String customerType = getCellValue(row.getCell(8));

                if (fullName.isBlank()) {
                    continue;
                }

                if (!phone.isBlank() && leadRepository.existsByPhone(phone)) {
                    continue;
                }

                if (!email.isBlank() && leadRepository.existsByEmail(email)) {
                    continue;
                }

                if (!taxCode.isBlank() && leadRepository.existsByTaxCode(taxCode)) {
                    continue;
                }

                Customer customer = new Customer();

                customer.setFullName(fullName);
                customer.setName(fullName);
                customer.setGender(gender);
                customer.setPhone(phone);
                customer.setEmail(email);
                customer.setAddress(address);
                customer.setCompanyName(companyName);
                customer.setTaxCode(taxCode);
                customer.setCustomerSource(customerSource);
                customer.setCustomerType(customerType);
                customer.setCustomerStatus("NEW");
                customer.setCreatedAt(LocalDateTime.now());
                customer.setCreatedBy(creator.getId());

                leadRepository.save(customer);
            }

        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage());
        }
    }
    @Override
    public void saveCustomer(Customer customer) {
        leadRepository.save(customer);
    }


    @Override
    public List<Customer> allCustomer() {
        return leadRepository.findAll(Sort.by("id").ascending());
    }
    @Override
    public List<User> findSale(String roleName) {
        return userRepository.findByRole_RoleName(roleName);
    }
    @Override
    public Customer findCustomerById(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
    @Override
    public User findUser(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @Override
    @Transactional
    public void assignCustomersToSale(List<Long> customerIds, Long saleId) {
        if (customerIds == null || customerIds.isEmpty()) {
            return;
        }

        User sale = userRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        for (Long customerId : customerIds) {
            Customer customer = findCustomerById(customerId);
            customer.setAssignedSalesId(sale.getId());
            leadRepository.save(customer);

            Opportunity opportunity = opportunityRepository.findByCustomerId(customer.getId())
                    .orElseGet(() -> Opportunity.builder()
                            .opportunityCode(buildOpportunityCode(customer))
                            .customer(customer)
                            .stage("NEW")
                            .build());

            opportunity.setAssignedTo(sale);
            opportunityRepository.save(opportunity);
        }
    }

    private String buildOpportunityCode(Customer customer) {
        return "OPP-CUS-%06d".formatted(customer.getId());
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
