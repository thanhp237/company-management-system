package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.CustomerActivityRepository;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerReportScopeService {

    private static final List<Contract.ContractStatus> ADMIN_OFFICER_POOLED_STATUSES =
            List.of(Contract.ContractStatus.PENDING_ADMIN_OFFICER);

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerActivityRepository activityRepository;

    public List<Contract> visibleContracts(Long customerId, Authentication authentication) {
        User user = currentUser(authentication);
        String roleCode = roleCode(user);
        Employee employee = currentEmployee(authentication);

        if (canViewAll(roleCode)) {
            return contractRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        }
        if (employee == null) {
            return List.of();
        }
        if ("ACCOUNTANT".equals(roleCode)) {
            return contractRepository.findByCustomerIdAndHasScopedInvoiceOrderByCreatedAtDesc(customerId, employee.getId());
        }
        if ("ADMIN_OFFICER".equals(roleCode) || "ADMINOFFICER".equals(roleCode)) {
            return contractRepository.findByCustomerIdAndAdminOfficerScopeOrderByCreatedAtDesc(
                    customerId,
                    employee.getId(),
                    ADMIN_OFFICER_POOLED_STATUSES);
        }
        if ("SALES".equals(roleCode)) {
            return contractRepository.findByCustomerIdAndSaleIdOrderByCreatedAtDesc(customerId, employee.getId());
        }
        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            List<Long> saleIds = departmentSaleEmployeeIds(user);
            return saleIds.isEmpty()
                    ? List.of()
                    : contractRepository.findByCustomerIdAndSaleIdInOrderByCreatedAtDesc(customerId, saleIds);
        }

        return List.of();
    }

    public List<Invoice> visibleInvoices(Long customerId, Authentication authentication) {
        User user = currentUser(authentication);
        String roleCode = roleCode(user);
        Employee employee = currentEmployee(authentication);

        if (canViewAll(roleCode)) {
            return invoiceRepository.findByContractCustomerIdOrderByCreatedAtDesc(customerId);
        }
        if (employee == null) {
            return List.of();
        }
        if ("ACCOUNTANT".equals(roleCode)) {
            return invoiceRepository.findByContractCustomerIdAndAccountantOrderByCreatedAtDesc(customerId, employee.getId());
        }
        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            List<Long> saleIds = departmentSaleEmployeeIds(user);
            return saleIds.isEmpty()
                    ? List.of()
                    : invoiceRepository.findByContractCustomerIdAndContractSaleIdInOrderByCreatedAtDesc(customerId, saleIds);
        }

        return List.of();
    }

    public List<CustomerActivity> visibleActivities(Long customerId, Authentication authentication) {
        User user = currentUser(authentication);
        String roleCode = roleCode(user);
        Employee employee = currentEmployee(authentication);

        if (canViewAll(roleCode) || "MARKETING".equals(roleCode)) {
            return activityRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        }
        if (employee == null) {
            return List.of();
        }
        if ("SALES".equals(roleCode)) {
            return activityRepository.findByCustomerIdAndEmployeeIdOrderByCreatedAtDesc(customerId, employee.getId());
        }
        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            List<Long> employeeIds = departmentSaleEmployeeIds(user);
            return employeeIds.isEmpty()
                    ? List.of()
                    : activityRepository.findByCustomerIdAndEmployeeIdInOrderByCreatedAtDesc(customerId, employeeIds);
        }
        if ("ADMIN_OFFICER".equals(roleCode) || "ADMINOFFICER".equals(roleCode)) {
            return customerRepository.existsContractForAdminOfficerScopeAndCustomer(
                    employee.getId(),
                    customerId,
                    ADMIN_OFFICER_POOLED_STATUSES)
                    ? activityRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                    : List.of();
        }
        if ("ACCOUNTANT".equals(roleCode)) {
            return customerRepository.existsInvoiceForCustomerId(customerId, employee.getId())
                    ? activityRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                    : List.of();
        }

        return List.of();
    }

    public Page<CustomerActivity> visibleActivityPage(Long customerId,
                                                      String activityType,
                                                      int page,
                                                      int size,
                                                      Authentication authentication) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (customerId != null) {
            List<CustomerActivity> activities = visibleActivities(customerId, authentication)
                    .stream()
                    .filter(activity -> matchesType(activity, activityType))
                    .toList();
            return pageFromList(activities, pageable);
        }

        List<Long> customerIds = visibleCustomerIds(authentication);
        if (customerIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (activityType == null || activityType.isBlank()) {
            return activityRepository.findByCustomerIdIn(customerIds, pageable);
        }
        return activityRepository.findByCustomerIdInAndActivityTypeIgnoreCase(customerIds, activityType, pageable);
    }

    public boolean canViewContract(Long contractId, Authentication authentication) {
        User user = currentUser(authentication);
        String roleCode = roleCode(user);
        if ("ADMIN".equals(roleCode) || "DIRECTOR".equals(roleCode) || "ACCOUNTANT".equals(roleCode)) {
            return true;
        }

        Employee employee = currentEmployee(authentication);
        if (employee == null) {
            return false;
        }

        return contractRepository.findById(contractId)
                .map(contract -> canViewScopedContract(contract, user, employee, roleCode))
                .orElse(false);
    }

    public boolean canViewInvoice(Long invoiceId, Authentication authentication) {
        User user = currentUser(authentication);
        String roleCode = roleCode(user);
        Employee employee = currentEmployee(authentication);

        return invoiceRepository.findById(invoiceId)
                .map(invoice -> {
                    if (canViewAll(roleCode)) {
                        return true;
                    }
                    if ("ACCOUNTANT".equals(roleCode)) {
                        boolean ownsInvoice = employee != null
                                && (Objects.equals(invoice.getCreatedBy(), employee.getId())
                                || Objects.equals(invoice.getUpdatedBy(), employee.getId()));
                        return ownsInvoice || Invoice.InvoiceStatus.PAYMENT_PENDING.equals(invoice.getStatus());
                    }
                    return invoice.getContract() != null
                            && invoice.getContract().getCustomer() != null
                            && visibleInvoices(invoice.getContract().getCustomer().getId(), authentication).stream()
                                    .anyMatch(scopedInvoice -> Objects.equals(scopedInvoice.getId(), invoiceId));
                })
                .orElse(false);
    }

    public boolean canViewActivity(Long activityId, Authentication authentication) {
        return activityRepository.findById(activityId)
                .map(activity -> visibleActivities(activity.getCustomerId(), authentication).stream()
                        .anyMatch(scopedActivity -> Objects.equals(scopedActivity.getId(), activityId)))
                .orElse(false);
    }

    private List<Long> visibleCustomerIds(Authentication authentication) {
        User user = currentUser(authentication);
        String roleCode = roleCode(user);
        Employee employee = currentEmployee(authentication);

        if (canViewAll(roleCode) || "MARKETING".equals(roleCode)) {
            return customerRepository.findAllByOrderByCreatedAtDesc().stream().map(customer -> customer.getId()).toList();
        }
        if (employee == null) {
            return List.of();
        }
        if ("SALES".equals(roleCode)) {
            return customerRepository.findByAssignedSalesIdOrderByCreatedAtDesc(user.getId()).stream()
                    .map(customer -> customer.getId())
                    .toList();
        }
        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            List<Long> userIds = departmentSaleUserIds(user);
            return userIds.isEmpty()
                    ? List.of()
                    : customerRepository.findByAssignedSalesIdInOrderByCreatedAtDesc(userIds).stream()
                            .map(customer -> customer.getId())
                            .toList();
        }
        if ("ADMIN_OFFICER".equals(roleCode) || "ADMINOFFICER".equals(roleCode)) {
            return customerRepository.findCustomersByAdminOfficerScopeOrderByCreatedAtDesc(
                            employee.getId(),
                            null,
                            ADMIN_OFFICER_POOLED_STATUSES)
                    .stream()
                    .map(customer -> customer.getId())
                    .toList();
        }
        if ("ACCOUNTANT".equals(roleCode)) {
            return customerRepository.findCustomersWithInvoicesOrderByCreatedAtDesc(null, employee.getId())
                    .stream()
                    .map(customer -> customer.getId())
                    .toList();
        }

        return List.of();
    }

    private List<Long> departmentSaleEmployeeIds(User user) {
        if (user == null || user.getDepartmentId() == null) {
            return List.of();
        }
        return userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(user.getDepartmentId())
                .stream()
                .filter(deptUser -> "SALES".equals(roleCode(deptUser)))
                .map(User::getEmployee)
                .filter(employee -> employee != null && employee.getId() != null)
                .map(Employee::getId)
                .toList();
    }

    private boolean canViewScopedContract(Contract contract, User user, Employee employee, String roleCode) {
        if ("SALES".equals(roleCode)) {
            return contract.getSale() != null && Objects.equals(contract.getSale().getId(), employee.getId());
        }
        if ("MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode)) {
            List<Long> employeeIds = departmentEmployeeIds(user);
            return !employeeIds.isEmpty()
                    && ((contract.getSale() != null && employeeIds.contains(contract.getSale().getId()))
                    || (contract.getAdminOfficer() != null && employeeIds.contains(contract.getAdminOfficer().getId())));
        }
        if ("ADMIN_OFFICER".equals(roleCode) || "ADMINOFFICER".equals(roleCode)) {
            return (contract.getAdminOfficer() != null && Objects.equals(contract.getAdminOfficer().getId(), employee.getId()))
                    || ADMIN_OFFICER_POOLED_STATUSES.contains(contract.getStatus());
        }

        return false;
    }

    private List<Long> departmentEmployeeIds(User user) {
        if (user == null || user.getDepartmentId() == null) {
            return List.of();
        }
        return userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(user.getDepartmentId())
                .stream()
                .map(User::getEmployee)
                .filter(employee -> employee != null && employee.getId() != null)
                .map(Employee::getId)
                .toList();
    }

    private List<Long> departmentSaleUserIds(User user) {
        if (user == null || user.getDepartmentId() == null) {
            return List.of();
        }
        return userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(user.getDepartmentId())
                .stream()
                .filter(deptUser -> "SALES".equals(roleCode(deptUser)))
                .map(User::getId)
                .toList();
    }

    private boolean matchesType(CustomerActivity activity, String activityType) {
        return activityType == null || activityType.isBlank()
                || (activity.getActivityType() != null && activity.getActivityType().equalsIgnoreCase(activityType));
    }

    private Page<CustomerActivity> pageFromList(List<CustomerActivity> activities, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), activities.size());
        int end = Math.min(start + pageable.getPageSize(), activities.size());
        return new PageImpl<>(activities.subList(start, end), pageable, activities.size());
    }

    private boolean canViewAll(String roleCode) {
        return "ADMIN".equals(roleCode) || "DIRECTOR".equals(roleCode);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    private Employee currentEmployee(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return employeeRepository.findByUser_Username(authentication.getName()).orElse(null);
    }

    private String roleCode(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleCode() == null) {
            return "";
        }
        return user.getRole().getRoleCode().trim().toUpperCase();
    }
}
