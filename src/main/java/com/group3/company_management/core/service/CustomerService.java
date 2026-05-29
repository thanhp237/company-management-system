package com.group3.company_management.core.service;

// src/main/java/com/group3/company_management/customer/service/CustomerService.java

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.group3.company_management.core.dto.CreateCustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.core.dto.UpdateCustomerRequest;

public interface CustomerService {
    
    Page<CustomerResponse> getAllCustomers(Pageable pageable);
    
    Page<CustomerResponse> searchCustomers(String searchTerm, Pageable pageable);
    
    Page<CustomerResponse> filterByStatus(String status, Pageable pageable);
    
    Page<CustomerResponse> searchAndFilter(String searchTerm, String status, Pageable pageable);
    
    CustomerResponse getCustomerById(Long id);
    
    CustomerResponse createCustomer(CreateCustomerRequest request);
    
    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);
    
    void deleteCustomer(Long id);
}