package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.AppointmentRepository;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.DepartmentRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.NotificationRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.repository.RoleRepository;
import com.group3.company_management.core.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dashboard controller for authenticated users
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContractRepository contractRepository;
    private final ProductRepository productRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final QuotationRepository quotationRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String redirectDashboard(Authentication authentication) {
        return "redirect:" + dashboardUrl(authentication);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "ADMIN", "Admin Dashboard");
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('SALES')")
    public String salesDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "SALES", "Sales Dashboard");
    }

    @GetMapping("/marketing")
    @PreAuthorize("hasRole('MARKETING')")
    public String marketingDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "MARKETING", "Marketing Dashboard");
    }

    @GetMapping("/accountant")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String accountantDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "ACCOUNTANT", "Accountant Dashboard");
    }

    @GetMapping("/admin-officer")
    @PreAuthorize("hasAnyRole('ADMIN_OFFICER', 'ADMINOFFICER')")
    public String adminOfficerDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "ADMIN_OFFICER", "Admin Officer Dashboard");
    }

    @GetMapping("/sales-manager")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'MANAGER')")
    public String salesManagerDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "SALES_MANAGER", "Sales Manager Dashboard");
    }

    @GetMapping("/director")
    @PreAuthorize("hasRole('DIRECTOR')")
    public String directorDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "DIRECTOR", "Director Dashboard");
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','ADMINOFFICER','SALES','MARKETING','SALES_MANAGER','MANAGER','ACCOUNTANT','DIRECTOR')")
    public String employeeDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, normalizeEmployeeRole(authentication), "Employee Dashboard");
    }

    /**
     * =========================
     * CUSTOMER DASHBOARD
     * =========================
     */
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerDashboard(Authentication authentication, Model model) {

        model.addAttribute("title", "Customer Dashboard");
        model.addAttribute("userName", authentication.getName());

        Optional<Customer> customer = customerRepository.findByEmailAndNotDeleted(authentication.getName());
        Long customerId = customer.map(Customer::getId).orElse(null);
        long quoteCount = customerId == null ? 0 : quotationRepository.countByCustomerId(customerId);
        long contractCount = customerId == null ? 0 : contractRepository.countByCustomerId(customerId);
        long signedContracts = customerId == null
                ? 0
                : contractRepository.countByCustomerIdAndStatus(customerId, Contract.ContractStatus.SIGNED);

        model.addAttribute("kpiCards", List.of(
                card("Báo giá", number(quoteCount), "Báo giá đang theo dõi trong portal", "fa-file-invoice", "info"),
                card("Hợp đồng", number(contractCount), "Hợp đồng thuộc tài khoản khách hàng", "fa-file-contract", "success"),
                card("Đã ký", number(signedContracts), "Hợp đồng hoàn tất ký kết", "fa-file-signature", "warning")
        ));
        model.addAttribute("actionGroups", List.of(
                group("Không gian khách hàng", "Theo dõi tài liệu và trạng thái dịch vụ của bạn.", List.of(
                        action("Cổng khách hàng", "/customer/portal", "Xem tổng quan hồ sơ khách hàng", "fa-house-user"),
                        action("Hợp đồng của tôi", "/customer/portal/contracts", "Theo dõi hợp đồng đã ký và đang xử lý", "fa-file-signature"),
                        action("Báo giá của tôi", "/customer/portal/quotes", "Xem báo giá và phản hồi chấp thuận", "fa-file-invoice-dollar"),
                        action("Thanh toán", "/customer/portal/payments", "Theo dõi lịch sử thanh toán", "fa-wallet")
                ))
        ));
        model.addAttribute("insightItems", List.of(
                insight("Truy cập dành riêng cho khách hàng", "Customer chỉ xem dữ liệu portal của chính mình."),
                insight("Không truy cập CRM nội bộ", "Các màn Users, Pipeline, Contracts nội bộ được ẩn khỏi Customer.")
        ));

        return "dashboard/customer-dashboard";
    }

    private String employeeDashboard(Authentication authentication, Model model, String role, String title) {
        model.addAttribute("title", title);
        model.addAttribute("userName", authentication.getName());
        model.addAttribute("role", role);

        model.addAttribute("roleLabel", roleLabel(role));
        model.addAttribute("dashboardSubtitle", dashboardSubtitle(role));
        model.addAttribute("kpiCards", kpiCards(role, authentication));
        model.addAttribute("actionGroups", actionGroups(role));
        model.addAttribute("insightItems", insightItems(role));

        return "dashboard/employee-dashboard";
    }

    private List<Map<String, Object>> kpiCards(String role, Authentication authentication) {
        User user = currentUser(authentication);
        Employee employee = currentEmployee(authentication);
        Long employeeId = employee == null ? null : employee.getId();
        String username = authentication == null ? "" : authentication.getName();

        return switch (role) {
            case "ADMIN" -> List.of(
                    card("Tài khoản", number(userRepository.count()), "Tài khoản chưa xóa mềm", "fa-users-gear", "success"),
                    card("Vai trò active", number(roleRepository.findByStatusIgnoreCaseOrderByRoleNameAsc("ACTIVE").size()), "Role có thể gán cho nhân sự", "fa-user-shield", "info"),
                    card("Phòng ban", number(departmentRepository.findByIsDeletedFalseOrderByNameAsc().size()), "Cơ cấu tổ chức đang hoạt động", "fa-sitemap", "warning"),
                    card("Thông báo", number(unreadNotifications(user)), "Thông báo chưa đọc", "fa-bell", "danger")
            );
            case "MARKETING" -> List.of(
                    card("Khách hàng", number(customerRepository.count()), "Tổng hồ sơ khách hàng/lead", "fa-address-book", "success"),
                    card("Active", number(customerRepository.countByCustomerStatusIgnoreCase("ACTIVE")), "Khách hàng đang hoạt động", "fa-user-check", "info"),
                    card("Pipeline", number(opportunityRepository.count()), "Opportunity đã sinh từ lead", "fa-chart-line", "warning"),
                    card("Thông báo", number(unreadNotifications(user)), "Thông báo chưa đọc", "fa-bell", "danger")
            );
            case "SALES" -> List.of(
                    card("Khách hàng của tôi", number(myCustomerCount(user)), "Danh sách được phân bổ", "fa-user-check", "success"),
                    card("Opportunity", number(opportunityRepository.countByAssignedToUsername(username)), "Pipeline cá nhân", "fa-chart-line", "info"),
                    card("Hợp đồng", number(employeeId == null ? 0 : contractRepository.countBySaleId(employeeId)), "Hợp đồng phụ trách", "fa-file-contract", "warning"),
                    card("Lịch hẹn", number(appointmentRepository.countByEmployeeUsername(username)), "Cuộc hẹn cần theo dõi", "fa-calendar-check", "danger")
            );
            case "SALES_MANAGER" -> List.of(
                    card("Pipeline nhóm", number(opportunityRepository.count()), "Cơ hội toàn đội", "fa-chart-column", "success"),
                    card("Won deals", number(opportunityRepository.countByStage("WON")), "Opportunity đã thắng", "fa-trophy", "info"),
                    card("Hợp đồng", number(contractRepository.count()), "Tiến độ ký kết", "fa-file-signature", "warning"),
                    card("Pending review", number(contractRepository.countByStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER)), "Hợp đồng chờ thẩm định", "fa-hourglass-half", "danger")
            );
            case "ADMIN_OFFICER" -> List.of(
                    card("Chờ thẩm định", number(contractRepository.countByStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER)), "Hợp đồng pending", "fa-clipboard-check", "warning"),
                    card("Đã rà soát", number(employeeId == null ? 0 : contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.ADMIN_REVIEWED)), "Hồ sơ đã xử lý bởi bạn", "fa-scale-balanced", "success"),
                    card("Được phân công", number(employeeId == null ? 0 : contractRepository.countByAdminOfficerId(employeeId)), "Hợp đồng đã nhận xử lý", "fa-file-pen", "info"),
                    card("Thông báo", number(unreadNotifications(user)), "Việc cần phản hồi", "fa-bell", "danger")
            );
            case "ACCOUNTANT" -> List.of(
                    card("Doanh thu ký", money(contractRepository.sumFinalAmountByStatus(Contract.ContractStatus.SIGNED)), "Tổng giá trị hợp đồng đã ký", "fa-coins", "success"),
                    card("Hợp đồng gửi KH", number(contractRepository.countByStatus(Contract.ContractStatus.SENT_TO_CUSTOMER)), "Chờ khách ký/thanh toán", "fa-receipt", "warning"),
                    card("Đã ký", number(contractRepository.countByStatus(Contract.ContractStatus.SIGNED)), "Cơ sở ghi nhận doanh thu", "fa-file-invoice", "info"),
                    card("Hoa hồng", "N/A", "Commission module chưa có entity", "fa-hand-holding-dollar", "danger")
            );
            case "DIRECTOR" -> List.of(
                    card("Doanh thu", money(contractRepository.sumFinalAmountByStatus(Contract.ContractStatus.SIGNED)), "Business performance từ hợp đồng đã ký", "fa-chart-pie", "success"),
                    card("Hợp đồng", number(contractRepository.count()), "Tổng hợp toàn hệ thống", "fa-file-contract", "info"),
                    card("Won deals", number(opportunityRepository.countByStage("WON")), "Opportunity thắng", "fa-ranking-star", "warning"),
                    card("Lost deals", number(opportunityRepository.countByStage("LOST")), "Opportunity mất", "fa-arrow-trend-down", "danger")
            );
            default -> List.of(
                    card("Tổng quan", "0", "Dữ liệu theo quyền truy cập", "fa-chart-line", "success"),
                    card("Thông báo", "0", "Công việc mới", "fa-bell", "info"),
                    card("Hồ sơ", "OK", "Tài khoản đang hoạt động", "fa-user", "warning"),
                    card("Bảo mật", "OK", "Phiên đăng nhập hợp lệ", "fa-shield-halved", "danger")
            );
        };
    }

    private List<Map<String, Object>> actionGroups(String role) {
        return switch (role) {
            case "ADMIN" -> List.of(
                    group("Khởi tạo & cấu hình", "Thiết lập nhân sự, RBAC và cấu trúc tổ chức.", List.of(
                            action("User Management", "/users", "Tạo, khóa/mở khóa và phân quyền nhân sự", "fa-users"),
                            action("Role Management", "/roles", "Quản lý role RBAC", "fa-user-shield"),
                            action("Departments", "/departments", "Quản lý phòng ban", "fa-sitemap"),
                            action("Business Rules", "/business-rules", "Cấu hình luật chia lead và tham số nền", "fa-sliders")
                    ))
            );
            case "MARKETING" -> List.of(
                    group("Tiếp nhận lead", "Thu thập dữ liệu và chuyển lead cho đội sales.", List.of(
                            action("Import lead", "/customer", "Nhập lead từ file và kiểm tra dữ liệu", "fa-file-import"),
                            action("Notifications", "/notifications", "Theo dõi thông báo hệ thống", "fa-bell"),
                            disabledAction("Content Hub", "Soạn thảo nội dung tiếp cận đa kênh", "fa-bullhorn"),
                            disabledAction("Lead Quality Board", "Theo dõi lead trùng lặp và nguồn chiến dịch", "fa-chart-simple")
                    ))
            );
            case "SALES" -> List.of(
                    group("Tác nghiệp bán hàng", "Chăm sóc khách hàng, pipeline và hợp đồng cá nhân.", List.of(
                            action("Pipeline", "/pipeline", "Theo dõi cơ hội bán hàng", "fa-chart-line"),
                            action("Lịch sử tương tác", "/customer-activities", "Ghi nhận chăm sóc khách hàng", "fa-clock-rotate-left"),
                            action("Lịch hẹn", "/appointments", "Quản lý lịch chăm sóc", "fa-calendar-days"),
                            action("Hợp đồng", "/contracts", "Theo dõi hợp đồng phụ trách", "fa-file-contract")
                    ))
            );
            case "SALES_MANAGER" -> List.of(
                    group("Quản trị hiệu suất sales", "Giám sát pipeline, lead và hợp đồng của đội.", List.of(
                            action("Pipeline nhóm", "/pipeline", "Theo dõi opportunity toàn đội", "fa-chart-column"),
                            action("Khách hàng", "/customers", "Giám sát danh sách khách hàng", "fa-users"),
                            action("Hợp đồng", "/contracts", "Theo dõi tiến độ hợp đồng", "fa-file-signature"),
                            action("Lead distribution", "/customer", "Kiểm tra luồng phân bổ lead", "fa-people-arrows")
                    ))
            );
            case "ADMIN_OFFICER" -> List.of(
                    group("Rà soát hợp đồng", "Xử lý điều khoản pháp lý và vòng đời hợp đồng.", List.of(
                            action("Contract Workspace", "/contracts", "Xem hợp đồng chờ thẩm định", "fa-clipboard-check"),
                            action("Business Rules", "/business-rules", "Tham số nghiệp vụ liên quan thẩm định", "fa-scale-balanced"),
                            action("Departments", "/departments", "Tra cứu cấu trúc tổ chức", "fa-sitemap"),
                            action("Notifications", "/notifications", "Theo dõi thông báo xử lý", "fa-bell")
                    ))
            );
            case "ACCOUNTANT" -> List.of(
                    group("Tài chính & doanh thu", "Các phân hệ kế toán đang được kết nối theo đặc tả.", List.of(
                            disabledAction("Invoice Billing Hub", "Quản lý hóa đơn từ hợp đồng", "fa-file-invoice"),
                            disabledAction("Payment Receipts", "Ghi nhận thanh toán theo đợt", "fa-money-check-dollar"),
                            disabledAction("Commission Ledger", "Tính hoa hồng tự động", "fa-hand-holding-dollar"),
                            disabledAction("Financial Reports", "Xuất Excel/PDF báo cáo tài chính", "fa-file-export")
                    ))
            );
            case "DIRECTOR" -> List.of(
                    group("Executive analytics", "Theo dõi KPI, doanh thu và hiệu suất doanh nghiệp.", List.of(
                            disabledAction("CEO Executive Dashboard", "Biểu đồ doanh thu và biên lợi nhuận", "fa-chart-pie"),
                            disabledAction("Sales KPI Board", "Xếp hạng sales và conversion rate", "fa-ranking-star"),
                            disabledAction("Report Export Center", "Tải báo cáo Excel/PDF", "fa-file-export")
                    ))
            );
            default -> List.of(
                    group("Workspace", "Các chức năng khả dụng theo quyền hiện tại.", List.of(
                            action("Dashboard", "/dashboard", "Quay về tổng quan", "fa-chart-line"),
                            action("Profile", "/profile", "Thông tin cá nhân", "fa-user")
                    ))
            );
        };
    }

    private List<Map<String, String>> insightItems(String role) {
        return switch (role) {
            case "ADMIN" -> List.of(
                    insight("Data scope: ALL", "Administrator quản lý tài khoản, role, phòng ban và cấu hình nền."),
                    insight("Không thao tác sales trực tiếp", "Admin giám sát hệ thống, không thay thế luồng tác nghiệp của Sales.")
            );
            case "MARKETING" -> List.of(
                    insight("Lead ingestion", "Marketing tập trung nhập lead, kiểm tra trùng và chuyển dữ liệu cho Sales."),
                    insight("Không tạo hợp đồng", "Marketing không truy cập pipeline ký kết hoặc contract lifecycle.")
            );
            case "SALES" -> List.of(
                    insight("Data scope: OWN", "Sales thao tác trên khách hàng, opportunity và hợp đồng do mình phụ trách."),
                    insight("Contract từ pipeline", "Hợp đồng chỉ được tạo khi opportunity WON và quotation APPROVED/ACCEPTED.")
            );
            case "SALES_MANAGER" -> List.of(
                    insight("Data scope: GROUP", "Sales Manager theo dõi pipeline, lead và KPI của đội."),
                    insight("Không cấu hình RBAC", "Không truy cập User/Role Management.")
            );
            case "ADMIN_OFFICER" -> List.of(
                    insight("Không vào Pipeline", "Admin Officer xử lý hợp đồng pending, không quản lý opportunity sales."),
                    insight("Legal review", "Có quyền cập nhật điều khoản hợp đồng ở trạng thái chờ thẩm định.")
            );
            case "ACCOUNTANT" -> List.of(
                    insight("Financial scope", "Accountant theo dõi hóa đơn, thanh toán, công nợ và hoa hồng."),
                    insight("Module đang kết nối", "Các màn invoice/payment/report sẽ mở khi phân hệ tài chính hoàn tất.")
            );
            case "DIRECTOR" -> List.of(
                    insight("Executive view", "Director tập trung vào KPI doanh thu, hợp đồng và hiệu suất tổng thể."),
                    insight("Read-only dashboard", "Không trực tiếp thao tác dữ liệu vận hành hằng ngày.")
            );
            default -> List.of(
                    insight("Role-based dashboard", "Mỗi actor chỉ nhìn thấy khu vực phù hợp với quyền hạn.")
            );
        };
    }

    private String dashboardSubtitle(String role) {
        return switch (role) {
            case "ADMIN" -> "Khởi tạo hệ thống, RBAC, nhân sự và quy tắc nền tảng.";
            case "MARKETING" -> "Tiếp nhận lead, kiểm tra dữ liệu và hỗ trợ phân bổ khách hàng.";
            case "SALES" -> "Chăm sóc khách hàng, quản lý pipeline và hợp đồng cá nhân.";
            case "SALES_MANAGER" -> "Giám sát pipeline, phân bổ lead và KPI của đội sales.";
            case "ADMIN_OFFICER" -> "Rà soát pháp lý, bổ sung điều khoản và xử lý hợp đồng chờ thẩm định.";
            case "ACCOUNTANT" -> "Theo dõi hóa đơn, thanh toán, công nợ và hoa hồng.";
            case "DIRECTOR" -> "Tổng hợp KPI chiến lược và hiệu suất kinh doanh toàn công ty.";
            default -> "Không gian làm việc theo quyền hạn hiện tại.";
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "ADMIN" -> "Administrator";
            case "MARKETING" -> "Marketing Staff";
            case "SALES" -> "Sales Staff";
            case "SALES_MANAGER" -> "Sales Manager";
            case "ADMIN_OFFICER" -> "Admin Officer";
            case "ACCOUNTANT" -> "Accountant";
            case "DIRECTOR" -> "CEO / Director";
            default -> role;
        };
    }

    private Map<String, Object> group(String title, String description, List<Map<String, String>> actions) {
        return Map.of("title", title, "description", description, "actions", actions);
    }

    private Map<String, Object> card(String label, String value, String caption, String icon, String tone) {
        return Map.of("label", label, "value", value, "caption", caption, "icon", icon, "tone", tone);
    }

    private Map<String, String> action(String label, String href, String description, String icon) {
        return Map.of("label", label, "href", href, "description", description, "icon", icon, "disabled", "false");
    }

    private Map<String, String> disabledAction(String label, String description, String icon) {
        return Map.of("label", label, "href", "#", "description", description, "icon", icon, "disabled", "true");
    }

    private Map<String, String> insight(String title, String description) {
        return Map.of("title", title, "description", description);
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

    private long myCustomerCount(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }
        long ownerCount = customerRepository.countByOwnerId(user.getId());
        if (ownerCount > 0) {
            return ownerCount;
        }
        return customerRepository.countByAssignedSalesId(user.getId());
    }

    private long unreadNotifications(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }
        return notificationRepository.countByAccountIdAndIsReadFalse(user.getId());
    }

    private String number(long value) {
        return String.format("%,d", value);
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format("%,.0f VND", safeValue);
    }

    private String dashboardUrl(Authentication authentication) {
        String role = authority(authentication);
        return switch (role) {
            case "ROLE_ADMIN" -> "/dashboard/admin";
            case "ROLE_SALES" -> "/dashboard/sales";
            case "ROLE_MARKETING" -> "/dashboard/marketing";
            case "ROLE_ACCOUNTANT" -> "/dashboard/accountant";
            case "ROLE_ADMIN_OFFICER", "ROLE_ADMINOFFICER" -> "/dashboard/admin-officer";
            case "ROLE_SALES_MANAGER", "ROLE_MANAGER" -> "/dashboard/sales-manager";
            case "ROLE_DIRECTOR" -> "/dashboard/director";
            case "ROLE_CUSTOMER" -> "/dashboard/customer";
            default -> "/login";
        };
    }

    private String normalizeEmployeeRole(Authentication authentication) {
        String role = authority(authentication).replace("ROLE_", "");
        if ("ADMINOFFICER".equals(role)) {
            return "ADMIN_OFFICER";
        }
        if ("MANAGER".equals(role)) {
            return "SALES_MANAGER";
        }
        return role;
    }

    private String authority(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("");
    }
}
