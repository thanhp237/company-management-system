package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/core/controller/CustomerController.java



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.group3.company_management.core.dto.CreateCustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.core.dto.UpdateCustomerRequest;
import com.group3.company_management.core.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("isAuthenticated()")
public class CustomerController {
    
    private final CustomerService customerService;
    
    /**
     * GET /api/v1/customers
     * Get all customers with pagination
     */
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<CustomerResponse> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(customers);
    }
    
    /**
     * GET /api/v1/customers/search
     * Search customers by name or phone
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        if (q != null && !q.isEmpty() && status != null && !status.isEmpty()) {
            Page<CustomerResponse> customers = customerService.searchAndFilter(q, status, pageable);
            return ResponseEntity.ok(customers);
        }
        
        if (q != null && !q.isEmpty()) {
            Page<CustomerResponse> customers = customerService.searchCustomers(q, pageable);
            return ResponseEntity.ok(customers);
        }
        
        if (status != null && !status.isEmpty()) {
            Page<CustomerResponse> customers = customerService.filterByStatus(status, pageable);
            return ResponseEntity.ok(customers);
        }
        
        Page<CustomerResponse> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(customers);
    }
    
    /**
     * GET /api/v1/customers/{id}
     * Get customer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        try {
            CustomerResponse customer = customerService.getCustomerById(id);
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * POST /api/v1/customers
     * Create new customer
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
        try {
            CustomerResponse customer = customerService.createCustomer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * PUT /api/v1/customers/{id}
     * Update customer
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        
        try {
            CustomerResponse customer = customerService.updateCustomer(id, request);
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * DELETE /api/v1/customers/{id}
     * Delete customer (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}