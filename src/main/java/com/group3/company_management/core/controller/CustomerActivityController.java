package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.service.CustomerActivityService;
import com.group3.company_management.core.service.CustomerReportScopeService;
import com.group3.company_management.core.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer-activities")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MARKETING', 'SALES', 'MANAGER', 'SALES_MANAGER', 'ADMIN', 'ADMIN_OFFICER', 'ADMINOFFICER', 'ACCOUNTANT', 'DIRECTOR')")
public class CustomerActivityController {

    private static final java.util.Set<String> CLOSED_STAGES = java.util.Set.of("WON", "LOST");
    private static final java.util.Set<String> INTERACTION_TYPES = java.util.Set.of("CALL", "MEETING", "EMAIL");

    private final CustomerActivityService activityService;
    private final CustomerService customerService;
    private final CustomerReportScopeService customerReportScopeService;
    private final EmployeeRepository employeeRepository;
    private final OpportunityRepository opportunityRepository;

    @GetMapping
    public String listActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long customerId,
            Authentication authentication,
            Model model) {

        Page<CustomerActivity> activityPage =
                customerReportScopeService.visibleActivityPage(
                        customerId,
                        type,
                        page,
                        10,
                        authentication);

        model.addAttribute(
                "activityPage",
                activityPage);

        model.addAttribute(
                "activities",
                activityPage.getContent());

        model.addAttribute(
                "selectedType",
                type);
        model.addAttribute(
                "customerId",
                customerId);
        if (customerId != null) {
            try {
                model.addAttribute(
                        "customer",
                        customerService.getCustomerById(customerId));
            } catch (RuntimeException exception) {
                model.addAttribute("errorMessage", exception.getMessage());
                model.addAttribute("customerId", null);
            }
        }

        return "activity/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES', 'MANAGER', 'SALES_MANAGER', 'ADMIN')")
    public String showAddForm(
            @RequestParam Long customerId,
            @RequestParam(required = false) String relatedType,
            @RequestParam(required = false) Long relatedId,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            Model model) {

        CustomerActivity activity = new CustomerActivity();
        activity.setCustomerId(customerId);
        activity.setRelatedType(relatedType);
        activity.setRelatedId(relatedId);
        java.util.Optional<Employee> currentEmployee = currentEmployee(authentication);
        currentEmployee.ifPresent(employee -> activity.setEmployeeId(employee.getId()));

        model.addAttribute("activity", activity);
        model.addAttribute("customer", customerService.getCustomerById(customerId));
        model.addAttribute("currentEmployee", currentEmployee.orElse(null));
        model.addAttribute("currentEmployeeName", currentEmployee.map(employee -> employeeDisplayName(employee, authentication)).orElse("Không tìm thấy nhân viên đăng nhập"));
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl, "/customers/" + customerId));

        return "activity/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES', 'MANAGER', 'SALES_MANAGER', 'ADMIN')")
    public String saveActivity(
            @ModelAttribute("activity") CustomerActivity activity,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            customerService.getCustomerById(activity.getCustomerId());
            Employee employee = currentEmployee(authentication)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ nhân viên của tài khoản đang đăng nhập."));
            activity.setActivityType(normalizeRequired(activity.getActivityType(), "Loại hoạt động là bắt buộc"));
            validateActivityAllowed(activity);
            activity.setActivityNote(normalizeOptional(activity.getActivityNote()));
            activity.setRelatedType(normalizeOptional(activity.getRelatedType()));
            activity.setEmployeeId(employee.getId());
            activityService.save(activity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm hoạt động khách hàng.");
            return "redirect:" + safeReturnUrl(returnUrl, "/customers/" + activity.getCustomerId());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("activity", activity);
            model.addAttribute("errorMessage", exception.getMessage());
            if (activity.getCustomerId() != null) {
                model.addAttribute("customer", customerService.getCustomerById(activity.getCustomerId()));
            }
            java.util.Optional<Employee> currentEmployee = currentEmployee(authentication);
            model.addAttribute("currentEmployee", currentEmployee.orElse(null));
            model.addAttribute("currentEmployeeName", currentEmployee.map(employee -> employeeDisplayName(employee, authentication)).orElse("Không tìm thấy nhân viên đăng nhập"));
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl, "/customers"));
            return "activity/form";
        }
    }

    @GetMapping("/{id}")
    public String detailActivity(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (!customerReportScopeService.canViewActivity(id, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem tương tác này.");
            return "redirect:/customer-activities";
        }

        CustomerActivity activity =
                activityService.getActivityById(id);

        model.addAttribute(
                "activity",
                activity);

        return "activity/detail";
    }

    @PostMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES', 'MANAGER', 'SALES_MANAGER', 'ADMIN')")
    public String updateNote(
            @PathVariable Long id,
            @RequestParam String note,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes) {

        activityService.updateActivityNote(id, note);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ghi chú hoạt động.");

        return "redirect:" + safeReturnUrl(returnUrl, "/customer-activities/" + id);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeReturnUrl(String returnUrl, String fallback) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return fallback;
        }

        String normalized = returnUrl.trim();
        if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("://")) {
            return fallback;
        }

        return normalized;
    }

    private void validateActivityAllowed(CustomerActivity activity) {
        if (!INTERACTION_TYPES.contains(activity.getActivityType())) {
            return;
        }

        Opportunity opportunity = resolveRelatedOpportunity(activity);
        if (opportunity == null || opportunity.getStage() == null) {
            return;
        }

        String stage = opportunity.getStage().trim().toUpperCase();
        if (CLOSED_STAGES.contains(stage)) {
            throw new IllegalArgumentException("Cơ hội đã ở giai đoạn " + displayStage(stage) + ", không thể thêm tương tác cho thương vụ đã đóng.");
        }
    }

    private String displayStage(String stage) {
        if (stage == null) {
            return "chưa xác định";
        }
        return switch (stage.trim().toUpperCase()) {
            case "NEW" -> "Mới";
            case "QUALIFIED" -> "Đã đủ điều kiện";
            case "PROPOSAL" -> "Đề xuất";
            case "NEGOTIATION" -> "Đàm phán";
            case "WON" -> "Thắng";
            case "LOST" -> "Thua";
            default -> stage;
        };
    }

    private Opportunity resolveRelatedOpportunity(CustomerActivity activity) {
        if ("OPPORTUNITY".equalsIgnoreCase(activity.getRelatedType()) && activity.getRelatedId() != null) {
            return opportunityRepository.findDetailById(activity.getRelatedId()).orElse(null);
        }

        if (activity.getCustomerId() == null) {
            return null;
        }

        return opportunityRepository.findByCustomerIdOrderByUpdatedAtDesc(activity.getCustomerId())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private java.util.Optional<Employee> currentEmployee(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return java.util.Optional.empty();
        }
        return employeeRepository.findByUser_Username(authentication.getName());
    }

    private String employeeDisplayName(Employee employee, Authentication authentication) {
        if (employee.getEmployeeCode() != null && !employee.getEmployeeCode().isBlank()) {
            return employee.getEmployeeCode() + " - " + authentication.getName();
        }
        return authentication.getName();
    }
}
