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
    public void importCustomer(MultipartFile file,String name) {

        validateExcelFile(file);

        List<LeadDTO> leads = new ArrayList<>();

        try (Workbook workbook =
                     new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {


                Row row = sheet.getRow(i);


                if (row == null ||
                        !"Full Name".equalsIgnoreCase(getCellValue(row.getCell(0)))) {

                    throw new RuntimeException(
                            "Invalid Excel template. Please download and use the provided template."
                    );
                }
                if (row == null) {
                    continue;
                }
                if(leadRepository.existsByPhone(getCellValue(row.getCell(2)))){
                    continue;
                }if(leadRepository.existsByEmail(getCellValue(row.getCell(3)))){
                    continue;
                }if(leadRepository.existsByTaxCode(getCellValue(row.getCell(6)))){
                    continue;
                }



                if ((getCellValue(row.getCell(0))).isBlank()) {
                    continue;
                }
                LeadDTO dto = new LeadDTO();

                dto.setFullName(getCellValue(row.getCell(0)));
                dto.setGender(getCellValue(row.getCell(1)));
                dto.setPhone(getCellValue(row.getCell(2)));
                dto.setEmail(getCellValue(row.getCell(3)));
                dto.setAddress(getCellValue(row.getCell(4)));
                dto.setCompanyName(getCellValue(row.getCell(5)));
                dto.setTaxCode(getCellValue(row.getCell(6)));
                dto.setCustomerSource(getCellValue(row.getCell(7)));
                dto.setCustomerType(getCellValue(row.getCell(8)));

                leads.add(dto);
                Customer customer = new Customer();
                 customer.setFullName(dto.getFullName());
                customer.setPhone(dto.getPhone());
                customer.setEmail(dto.getEmail());
                customer.setAddress(dto.getAddress());
                customer.setCompanyName(dto.getCompanyName());
               customer.setTaxCode(dto.getTaxCode());
               customer.setCustomerSource(dto.getCustomerSource());
               customer.setCustomerType((dto.getCustomerType()));
               customer.setName(customer.getFullName());
                customer.setCreatedAt(LocalDateTime.now());
                customer.setGender(dto.getGender());
                customer.setCustomerStatus("NEW");
               customer.setCreatedBy(userRepository.findByUsername(name).get().getId());
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
