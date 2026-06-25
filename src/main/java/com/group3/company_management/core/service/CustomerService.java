package com.group3.company_management.core.service;

// src/main/java/com/group3/company_management/core/service/CustomerService.java

import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.customer.dto.CustomerPortalResponse;

import java.util.List;

/**
 * Customer service interface - same pattern as UserService
 */
public interface CustomerService {

    List<CustomerResponse> getAllCustomers();

    List<CustomerResponse> getActiveCustomers();

    List<CustomerResponse> getCustomersByStatus(String status);

    CustomerResponse getCustomerById(Long id);


    void createCustomer(CustomerRequest request);

    void updateCustomer(CustomerRequest request);

    void updateCustomerStatus(CustomerRequest request);

    void deleteCustomer(Long id);

    /**
     * Get customer info for portal dashboard
     */
    CustomerPortalResponse getCustomerPortalInfo(Long customerId);

    /**
     * Update customer profile (limited fields only)
     */
    void updateCustomerProfile(Long customerId, CustomerRequest request);
}