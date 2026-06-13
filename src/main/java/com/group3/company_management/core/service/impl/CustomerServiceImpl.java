package com.group3.company_management.core.service.impl;

// src/main/java/com/group3/company_management/core/service/impl/CustomerServiceImpl.java



import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer service implementation - same pattern as UserServiceImpl
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository customerRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");
        return customerRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getActiveCustomers() {
        log.info("Fetching active customers");
        return customerRepository.findByCustomerStatusOrderByCreatedAtDesc("ACTIVE")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByStatus(String status) {
        log.info("Fetching customers by status: {}", status);
        return customerRepository.findByCustomerStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        log.info("Fetching customer by ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
        return mapToResponse(customer);
    }
    
    @Override
    @Transactional
    public void createCustomer(CustomerRequest request) {
        log.info("Creating new customer: {}", request.getFullName());
        
        // Validate phone uniqueness
        if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        
        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .customerSource(request.getCustomerSource())
                .assignedSalesId(request.getAssignedSalesId())
                .customerStatus("ACTIVE")
                .build();
        
        customerRepository.save(customer);
        log.info("Customer created successfully: {}", request.getPhone());
    }
    
    @Override
    @Transactional
    public void updateCustomer(CustomerRequest request) {
        log.info("Updating customer with ID: {}", request.getId());
        
        Customer customer = customerRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getId()));
        
        // Validate phone uniqueness (if phone changed)
        if (!customer.getPhone().equals(request.getPhone())) {
            if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone number already exists");
            }
            customer.setPhone(request.getPhone());
        }
        
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setCustomerSource(request.getCustomerSource());
        customer.setAssignedSalesId(request.getAssignedSalesId());
        customer.setCustomerStatus(request.getCustomerStatus() != null ? request.getCustomerStatus() : "ACTIVE");
        
        customerRepository.save(customer);
        log.info("Customer updated successfully: {}", request.getId());
    }
    
    @Override
    @Transactional
    public void updateCustomerStatus(CustomerRequest request) {
        log.info("Updating customer status - ID: {}, Status: {}", request.getId(), request.getCustomerStatus());
        
        Customer customer = customerRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getId()));
        
        customer.setCustomerStatus(request.getCustomerStatus());
        customerRepository.save(customer);
        
        log.info("Customer status updated successfully: {}", request.getId());
    }
    
    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with ID: {}", id);
        
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }
        
        customerRepository.deleteById(id);
        log.info("Customer deleted successfully (soft delete): {}", id);
    }
    
    /**
     * Map Customer entity to CustomerResponse DTO
     */
    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .customerSource(customer.getCustomerSource())
                .assignedSalesId(customer.getAssignedSalesId())
                .customerStatus(customer.getCustomerStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}