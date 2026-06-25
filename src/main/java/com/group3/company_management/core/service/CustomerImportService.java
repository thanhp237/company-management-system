package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.LeadDTO;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CustomerImportService {
    void importCustomer(MultipartFile file,String name);
    List<Customer> allCustomer();
    List<User> findSale(String roleName);
    public Customer findCustomerById(Long id);
    public void saveCustomer(Customer customer);
    public User findUser(Long id);
    void assignCustomersToSale(List<Long> customerIds, Long saleId);
}
