// src/main/java/com/group3/company_management/core/service/impl/CustomerServiceImpl.java

package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.customer.dto.CustomerPortalResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Locale;

/**
 * Customer service implementation - UPDATED with portal methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    
    // ========== MANAGEMENT METHODS (Existing) ==========
    
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");
        return getFilteredCustomers(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getActiveCustomers() {
        log.info("Fetching active customers");
        return getFilteredCustomers("ACTIVE");
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByStatus(String status) {
        log.info("Fetching customers by status: {}", status);
        return getFilteredCustomers(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        log.info("Fetching customer by ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + id));
        if (!canAccessCustomer(customer)) {
            throw new AccessDeniedException("Bạn không có quyền xem hồ sơ khách hàng này.");
        }
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public void createCustomer(CustomerRequest request) {
        log.info("Creating new customer: {}", request.getFullName());

        // Validate phone uniqueness
        if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại");
        }

        // 1. Lấy ID người dùng đang tạo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long createdBy = null;
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> userOpt = userRepository.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                createdBy = userOpt.get().getId();
            }
        }

        // 2. Tạo đối tượng Customer với assignedSalesId = null (Chưa phân bổ)
        Customer customer = Customer.builder()
                .name(request.getFullName())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .customerSource(request.getCustomerSource())
                .assignedSalesId(null) // ✅ ĐẶT LÀ NULL ĐỂ CHƯA PHÂN BỔ
                .customerStatus("ACTIVE")
                .createdBy(createdBy)
                .build();

        customerRepository.save(customer);
        log.info("Customer created successfully: {}", request.getPhone());
    }
    @Override
    @Transactional
    public void updateCustomer(CustomerRequest request) {
        log.info("Updating customer with ID: {}", request.getId());
        
        Customer customer = customerRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + request.getId()));
        
         // Validate phone uniqueness (if phone changed)
        if (!customer.getPhone().equals(request.getPhone())) {
            if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Số điện thoại đã tồn tại");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + request.getId()));
        
        customer.setCustomerStatus(request.getCustomerStatus());
        customerRepository.save(customer);
        
        log.info("Customer status updated successfully: {}", request.getId());
    }
    
    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with ID: {}", id);
        
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + id);
        }
        
        customerRepository.deleteById(id);
        log.info("Customer deleted successfully (soft delete): {}", id);
    }
    
    // ========== CUSTOMER PORTAL METHODS (NEW) ==========
    
    @Override
    @Transactional(readOnly = true)
    public CustomerPortalResponse getCustomerPortalInfo(Long customerId) {
        log.info("Fetching customer portal info for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        
        return CustomerPortalResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .customerStatus(customer.getCustomerStatus())
                // TODO: Fetch these from Contract and Quote repositories
                .contractCount(0L)
                .pendingQuotesCount(0L)
                .totalPaidAmount(0L)
                .build();
    }
    
    @Override
    @Transactional
    public void updateCustomerProfile(Long customerId, CustomerRequest request) {
        log.info("Updating customer profile for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        
        // Only allow updating certain fields in portal
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        
        customerRepository.save(customer);
        log.info("Customer profile updated successfully: {}", customerId);
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
    @Override
    public  Customer findCustomerById(Long id){
        return customerRepository.findCustomerById(id);
    }
    @Override
    public  void saveCustomer(Customer customer){
        customerRepository.save(customer);
    }

    private List<CustomerResponse> getFilteredCustomers(String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }
        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User currentUser = userOpt.get();
        String roleCode = roleCode(currentUser);
        String normalizedStatus = normalizeStatus(status);

        if (canViewAllCustomers(roleCode)) {
            return loadCustomers(normalizedStatus).stream().map(this::mapToResponse).toList();
        }

        Optional<Employee> employeeOpt = employeeRepository.findByUser_Username(auth.getName());

        if ("ACCOUNTANT".equals(roleCode)) {
            if (employeeOpt.isEmpty()) {
                return List.of();
            }
            return customerRepository.findCustomersWithInvoicesOrderByCreatedAtDesc(normalizedStatus, employeeOpt.get().getId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (employeeOpt.isEmpty()) {
            return List.of();
        }
        Employee employee = employeeOpt.get();

        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            return loadDepartmentCustomers(currentUser, normalizedStatus).stream().map(this::mapToResponse).toList();
        }

        if ("ADMIN_OFFICER".equals(roleCode) || "ADMINOFFICER".equals(roleCode)) {
            return customerRepository.findCustomersByAdminOfficerIdOrderByCreatedAtDesc(employee.getId(), normalizedStatus)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if ("SALES".equals(roleCode)) {
            List<Customer> customers = (normalizedStatus != null)
                    ? customerRepository.findByCustomerStatusAndAssignedSalesIdOrderByCreatedAtDesc(normalizedStatus, currentUser.getId())
                    : customerRepository.findByAssignedSalesIdOrderByCreatedAtDesc(currentUser.getId());
            return customers.stream().map(this::mapToResponse).toList();
        }

        if ("MARKETING".equals(roleCode)) {
            List<Customer> customers = (normalizedStatus != null)
                    ? customerRepository.findByCustomerStatusOrderByCreatedAtDesc(normalizedStatus)
                    : customerRepository.findAllByOrderByCreatedAtDesc();
            return customers.stream().map(this::mapToResponse).toList();
        }

        return List.of();
    }

    private boolean canAccessCustomer(Customer customer) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return false;
        }
        User currentUser = userOpt.get();
        String roleCode = roleCode(currentUser);

        if (canViewAllCustomers(roleCode) || "MARKETING".equals(roleCode)) {
            return true;
        }

        if ("ACCOUNTANT".equals(roleCode)) {
            return employeeRepository.findByUser_Username(auth.getName())
                    .map(employee -> customerRepository.existsInvoiceForCustomerId(customer.getId(), employee.getId()))
                    .orElse(false);
        }

        Optional<Employee> employeeOpt = employeeRepository.findByUser_Username(auth.getName());
        if (employeeOpt.isEmpty()) {
            return false;
        }
        Employee employee = employeeOpt.get();

        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            if (customer.getAssignedSalesId() == null) {
                return true;
            }
            Long deptId = currentUser.getDepartmentId();
            if (deptId == null) {
                return employee.getId().equals(customer.getAssignedSalesId());
            }
            return userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(deptId)
                    .stream()
                    .filter(user -> "SALES".equals(roleCode(user)))
                    .map(User::getId)
                    .anyMatch(customer.getAssignedSalesId()::equals);
        }

        if ("ADMIN_OFFICER".equals(roleCode) || "ADMINOFFICER".equals(roleCode)) {
            return customerRepository.existsContractForAdminOfficerAndCustomer(employee.getId(), customer.getId());
        }

        if ("SALES".equals(roleCode)) {
            return currentUser.getId().equals(customer.getAssignedSalesId());
        }

        return false;
    }

    private List<Customer> loadDepartmentCustomers(User currentUser, String status) {
        Long deptId = currentUser.getDepartmentId();
        if (deptId == null) {
            return List.of();
        }


        List<User> deptUsers = userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(deptId);
        List<Long> employeeIds = new ArrayList<>();
        for (User user : deptUsers) {
            if ("SALES".equals(roleCode(user))) {
                employeeIds.add(user.getId());

            }
        }

        List<Customer> customers;
        if (employeeIds.isEmpty()) {
            customers = (status != null)
                    ? customerRepository.findByCustomerStatusAndAssignedSalesIdIsNullOrderByCreatedAtDesc(status)
                    : customerRepository.findByAssignedSalesIdIsNullOrderByCreatedAtDesc();
        } else if (status != null) {
            customers = new ArrayList<>(customerRepository.findByCustomerStatusAndAssignedSalesIdInOrderByCreatedAtDesc(status, employeeIds));
            customers.addAll(customerRepository.findByCustomerStatusAndAssignedSalesIdIsNullOrderByCreatedAtDesc(status));
        } else {
            customers = new ArrayList<>(customerRepository.findByAssignedSalesIdInOrderByCreatedAtDesc(employeeIds));
            customers.addAll(customerRepository.findByAssignedSalesIdIsNullOrderByCreatedAtDesc());
        }

        customers.sort((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()));
        return customers;
    }

    private List<Customer> loadCustomers(String status) {
        return (status != null)
                ? customerRepository.findByCustomerStatusOrderByCreatedAtDesc(status)
                : customerRepository.findAllByOrderByCreatedAtDesc();
    }

    private boolean canViewAllCustomers(String roleCode) {
        return "ADMIN".equals(roleCode) || "DIRECTOR".equals(roleCode);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return status.trim();
    }

    private String roleCode(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleCode() == null) {
            return "";
        }
        return user.getRole().getRoleCode().trim().toUpperCase(Locale.ROOT);
    }
}
