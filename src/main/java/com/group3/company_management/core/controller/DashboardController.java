package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.AppointmentRepository;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.DepartmentRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.InvoiceRepository;
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
    private final InvoiceRepository invoiceRepository;

    @GetMapping
    public String redirectDashboard(Authentication authentication) {
        return "redirect:" + dashboardUrl(authentication);
    }

    @GetMapping("/settings")
    public String settings() {
        return "dashboard/settings";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "ADMIN", "Bảng điều khiển quản trị");
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('SALES')")
    public String salesDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "SALES", "Bảng điều khiển kinh doanh");
    }

    @GetMapping("/marketing")
    @PreAuthorize("hasRole('MARKETING')")
    public String marketingDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "MARKETING", "Bảng điều khiển marketing");
    }

    @GetMapping("/accountant")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String accountantDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "ACCOUNTANT", "Bảng điều khiển kế toán");
    }

    @GetMapping("/admin-officer")
    @PreAuthorize("hasAnyRole('ADMIN_OFFICER', 'ADMINOFFICER')")
    public String adminOfficerDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "ADMIN_OFFICER", "Bảng điều khiển hành chính hợp đồng");
    }

    @GetMapping("/sales-manager")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'MANAGER')")
    public String salesManagerDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "SALES_MANAGER", "Bảng điều khiển quản lý kinh doanh");
    }

    @GetMapping("/director")
    @PreAuthorize("hasRole('DIRECTOR')")
    public String directorDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, "DIRECTOR", "Bảng điều khiển giám đốc");
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','ADMINOFFICER','SALES','MARKETING','SALES_MANAGER','MANAGER','ACCOUNTANT','DIRECTOR')")
    public String employeeDashboard(Authentication authentication, Model model) {
        return employeeDashboard(authentication, model, normalizeEmployeeRole(authentication), "Bảng điều khiển nhân viên");
    }

    /**
     * =========================
     * CUSTOMER DASHBOARD
     * =========================
     */
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerDashboard(Authentication authentication, Model model) {

        model.addAttribute("title", "Bảng điều khiển khách hàng");
        model.addAttribute("userName", authentication.getName());

        Optional<Customer> customer = customerRepository.findByEmailAndNotDeleted(authentication.getName());
        customer.ifPresent(value -> model.addAttribute("customer", value));
        Long customerId = customer.map(Customer::getId).orElse(null);
        long contractCount = customerId == null ? 0 : contractRepository.countByCustomerId(customerId);
        long signedContracts = customerId == null
                ? 0
                : contractRepository.countByCustomerIdAndStatus(customerId, Contract.ContractStatus.SIGNED);

        model.addAttribute("kpiCards", List.of(
                card("Hợp đồng", number(contractCount), "Hợp đồng thuộc tài khoản khách hàng", "fa-file-contract", "success"),
                card("Đã ký", number(signedContracts), "Hợp đồng hoàn tất ký kết", "fa-file-signature", "warning"),
                card("Thanh toán", "0", "Lịch sử thanh toán theo hợp đồng", "fa-wallet", "info")
        ));
        model.addAttribute("actionGroups", List.of(
                group("Không gian khách hàng", "Theo dõi tài liệu và trạng thái dịch vụ của bạn.", List.of(
                        action("Cổng khách hàng", "/customer/portal", "Xem tổng quan hồ sơ khách hàng", "fa-house-user"),
                        action("Hợp đồng của tôi", "/customer/portal/contracts", "Theo dõi hợp đồng đã ký và đang xử lý", "fa-file-signature"),
                        action("Thanh toán", "/customer/portal/payments", "Thanh toán hợp đồng và theo dõi lịch sử", "fa-wallet"),
                        action("Hồ sơ khách hàng", "/customer/portal/profile", "Cập nhật thông tin liên hệ của bạn", "fa-user-gear")
                ))
        ));
        model.addAttribute("insightItems", List.of(
                insight("Truy cập dành riêng cho khách hàng", "Khách hàng chỉ xem dữ liệu cổng thông tin của chính mình."),
                insight("Không truy cập CRM nội bộ", "Các màn nhân viên, quy trình bán hàng và hợp đồng nội bộ được ẩn khỏi khách hàng.")
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
        boolean executiveDashboard = isExecutiveRole(role);
        model.addAttribute("executiveDashboard", executiveDashboard);
        if (executiveDashboard) {
            model.addAttribute("revenueSummaryRows", revenueSummaryRows());
            model.addAttribute("executiveKpiRows", executiveKpiRows());
        }

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
                    card("Vai trò hoạt động", number(roleRepository.findByStatusIgnoreCaseOrderByRoleNameAsc("ACTIVE").size()), "Vai trò có thể gán cho nhân sự", "fa-user-shield", "info"),
                    card("Phòng ban", number(departmentRepository.findByIsDeletedFalseOrderByNameAsc().size()), "Cơ cấu tổ chức đang hoạt động", "fa-sitemap", "warning"),
                    card("Thông báo", number(unreadNotifications(user)), "Thông báo chưa đọc", "fa-bell", "danger")
            );
            case "MARKETING" -> List.of(
                    card("Khách hàng", number(customerRepository.count()), "Tổng hồ sơ khách hàng/lead", "fa-address-book", "success"),
                    card("Đang hoạt động", number(customerRepository.countByCustomerStatusIgnoreCase("ACTIVE")), "Khách hàng đang hoạt động", "fa-user-check", "info"),
                    card("Quy trình bán hàng", number(opportunityRepository.count()), "Cơ hội đã sinh từ khách hàng tiềm năng", "fa-chart-line", "warning"),
                    card("Thông báo", number(unreadNotifications(user)), "Thông báo chưa đọc", "fa-bell", "danger")
            );
            case "SALES" -> List.of(
                    card("Khách hàng của tôi", number(myCustomerCount(user)), "Danh sách được phân bổ", "fa-user-check", "success"),
                    card("Cơ hội", number(opportunityRepository.countByAssignedToUsername(username)), "Quy trình bán hàng cá nhân", "fa-chart-line", "info"),
                    card("Hợp đồng", number(employeeId == null ? 0 : contractRepository.countBySaleId(employeeId)), "Hợp đồng phụ trách", "fa-file-contract", "warning"),
                    card("Lịch hẹn", number(appointmentRepository.countByEmployeeUsername(username)), "Cuộc hẹn cần theo dõi", "fa-calendar-check", "danger")
            );
            case "SALES_MANAGER" -> {
                if (user.getDepartmentId() != null) {
                    List<User> deptUsers = userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(user.getDepartmentId());
                    List<String> deptUsernames = deptUsers.stream()
                            .filter(u -> !"MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) && 
                                         !"SALES_MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) &&
                                         !"ADMIN".equalsIgnoreCase(u.getRole().getRoleCode()))
                            .map(User::getUsername)
                            .toList();
                    List<Long> deptEmployeeIds = deptUsers.stream()
                            .filter(u -> !"MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) && 
                                         !"SALES_MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) &&
                                         !"ADMIN".equalsIgnoreCase(u.getRole().getRoleCode()))
                            .map(u -> u.getEmployee() != null ? u.getEmployee().getId() : null)
                            .filter(java.util.Objects::nonNull)
                            .toList();

                    long oppCount = deptUsernames.isEmpty() ? 0 : opportunityRepository.countByAssignedToUsernameIn(deptUsernames);
                    long wonCount = deptUsernames.isEmpty() ? 0 : opportunityRepository.countByStageAndAssignedToUsernameIn("WON", deptUsernames);
                    long contractCountVal = deptEmployeeIds.isEmpty() ? 0 : contractRepository.countBySaleIdIn(deptEmployeeIds);
                    long pendingContracts = deptEmployeeIds.isEmpty() ? 0 : contractRepository.countBySaleIdInAndStatus(deptEmployeeIds, Contract.ContractStatus.PENDING_ADMIN_OFFICER);

                    yield List.of(
                            card("Quy trình nhóm", number(oppCount), "Cơ hội của nhân viên trong phòng ban", "fa-chart-column", "success"),
                            card("Thương vụ thắng", number(wonCount), "Cơ hội đã thắng", "fa-trophy", "info"),
                            card("Hợp đồng", number(contractCountVal), "Tiến độ ký kết", "fa-file-signature", "warning"),
                            card("Chờ thẩm định", number(pendingContracts), "Hợp đồng chờ thẩm định", "fa-hourglass-half", "danger")
                    );
                } else {
                    yield List.of(
                            card("Quy trình nhóm", "0", "Cơ hội của nhân viên trong phòng ban", "fa-chart-column", "success"),
                            card("Thương vụ thắng", "0", "Cơ hội đã thắng", "fa-trophy", "info"),
                            card("Hợp đồng", "0", "Tiến độ ký kết", "fa-file-signature", "warning"),
                            card("Chờ thẩm định", "0", "Hợp đồng chờ thẩm định", "fa-hourglass-half", "danger")
                    );
                }
            }
            case "ADMIN_OFFICER" -> List.of(
                    card("Chờ thẩm định", number(contractRepository.countByStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER)), "Hợp đồng chờ xử lý", "fa-clipboard-check", "warning"),
                    card("Đã rà soát", number(employeeId == null ? 0 : contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.ADMIN_REVIEWED)), "Hồ sơ đã xử lý bởi bạn", "fa-scale-balanced", "success"),
                    card("Được phân công", number(employeeId == null ? 0 : contractRepository.countByAdminOfficerId(employeeId)), "Hợp đồng đã nhận xử lý", "fa-file-pen", "info"),
                    card("Thông báo", number(unreadNotifications(user)), "Việc cần phản hồi", "fa-bell", "danger")
            );
            case "ACCOUNTANT" -> List.of(
                    card("Doanh thu ký", money(contractRepository.sumFinalAmountByStatus(Contract.ContractStatus.SIGNED)), "Tổng giá trị hợp đồng đã ký", "fa-coins", "success"),
                    card("Hợp đồng gửi khách hàng", number(contractRepository.countByStatus(Contract.ContractStatus.SENT_TO_CUSTOMER)), "Chờ khách ký/thanh toán", "fa-receipt", "warning"),
                    card("Đã ký", number(contractRepository.countByStatus(Contract.ContractStatus.SIGNED)), "Cơ sở ghi nhận doanh thu", "fa-file-invoice", "info"),
                    card("Hoa hồng", "Chưa có", "Phân hệ hoa hồng chưa có dữ liệu", "fa-hand-holding-dollar", "danger")
            );
            case "DIRECTOR" -> List.of(
                    card("Doanh thu", money(contractRepository.sumFinalAmountByStatus(Contract.ContractStatus.SIGNED)), "Hiệu suất kinh doanh từ hợp đồng đã ký", "fa-chart-pie", "success"),
                    card("Hợp đồng", number(contractRepository.count()), "Tổng hợp toàn hệ thống", "fa-file-contract", "info"),
                    card("Thương vụ thắng", number(opportunityRepository.countByStage("WON")), "Cơ hội thắng", "fa-ranking-star", "warning"),
                    card("Thương vụ thua", number(opportunityRepository.countByStage("LOST")), "Cơ hội thua", "fa-arrow-trend-down", "danger")
            );
            default -> List.of(
                    card("Tổng quan", "0", "Dữ liệu theo quyền truy cập", "fa-chart-line", "success"),
                    card("Thông báo", "0", "Công việc mới", "fa-bell", "info"),
                    card("Hồ sơ", "Ổn", "Tài khoản đang hoạt động", "fa-user", "warning"),
                    card("Bảo mật", "Ổn", "Phiên đăng nhập hợp lệ", "fa-shield-halved", "danger")
            );
        };
    }

    private List<Map<String, Object>> actionGroups(String role) {
        return switch (role) {
            case "ADMIN" -> List.of(
                    group("Khởi tạo & cấu hình", "Thiết lập nhân sự, RBAC và cấu trúc tổ chức.", List.of(
                            action("Quản lý nhân viên", "/users", "Tạo, khóa/mở khóa và phân quyền nhân sự", "fa-users"),
                            action("Quản lý vai trò", "/roles", "Quản lý vai trò phân quyền", "fa-user-shield"),
                            action("Phòng ban", "/departments", "Quản lý phòng ban", "fa-sitemap"),
                            action("Quy tắc nghiệp vụ", "/business-rules", "Cấu hình luật chia khách hàng tiềm năng và tham số nền", "fa-sliders")
                    ))
            );
            case "MARKETING" -> List.of(
                    group("Tiếp nhận khách hàng tiềm năng", "Thu thập dữ liệu và chuyển khách hàng tiềm năng cho đội kinh doanh.", List.of(
                            action("Nhập khách hàng tiềm năng", "/customer", "Nhập dữ liệu từ file và kiểm tra thông tin", "fa-file-import"),
                            action("Thông báo", "/notifications", "Theo dõi thông báo hệ thống", "fa-bell"),
                            disabledAction("Kho nội dung", "Soạn thảo nội dung tiếp cận đa kênh", "fa-bullhorn"),
                            disabledAction("Bảng chất lượng khách hàng tiềm năng", "Theo dõi dữ liệu trùng lặp và nguồn chiến dịch", "fa-chart-simple")
                    ))
            );
            case "SALES" -> List.of(
                    group("Tác nghiệp bán hàng", "Chăm sóc khách hàng, pipeline và hợp đồng cá nhân.", List.of(
                            action("Quy trình bán hàng", "/pipeline", "Theo dõi cơ hội bán hàng", "fa-chart-line"),
                            action("Lịch sử tương tác", "/customer-activities", "Ghi nhận chăm sóc khách hàng", "fa-clock-rotate-left"),
                            action("Lịch hẹn", "/appointments", "Quản lý lịch chăm sóc", "fa-calendar-days"),
                            action("Hợp đồng", "/contracts", "Theo dõi hợp đồng phụ trách", "fa-file-contract")
                    ))
            );
            case "SALES_MANAGER" -> List.of(
                    group("Quản trị hiệu suất kinh doanh", "Giám sát quy trình bán hàng, khách hàng tiềm năng và hợp đồng của đội.", List.of(
                            action("Quy trình nhóm", "/pipeline", "Theo dõi cơ hội toàn đội", "fa-chart-column"),
                            action("Khách hàng", "/customers", "Giám sát danh sách khách hàng", "fa-users"),
                            action("Hợp đồng", "/contracts", "Theo dõi tiến độ hợp đồng", "fa-file-signature"),
                            action("Phân bổ khách hàng tiềm năng", "/customer", "Kiểm tra luồng phân bổ khách hàng tiềm năng", "fa-people-arrows")
                    ))
            );
            case "ADMIN_OFFICER" -> List.of(
                    group("Rà soát hợp đồng", "Xử lý điều khoản pháp lý và vòng đời hợp đồng.", List.of(
                            action("Khu vực hợp đồng", "/contracts", "Xem hợp đồng chờ thẩm định", "fa-clipboard-check"),
                            action("Quy tắc nghiệp vụ", "/business-rules", "Tham số nghiệp vụ liên quan thẩm định", "fa-scale-balanced"),
                            action("Phòng ban", "/departments", "Tra cứu cấu trúc tổ chức", "fa-sitemap"),
                            action("Thông báo", "/notifications", "Theo dõi thông báo xử lý", "fa-bell")
                    ))
            );
            case "ACCOUNTANT" -> List.of(
                    group("Tài chính & doanh thu", "Các phân hệ kế toán đang được kết nối theo đặc tả.", List.of(
                            disabledAction("Trung tâm hóa đơn", "Quản lý hóa đơn từ hợp đồng", "fa-file-invoice"),
                            disabledAction("Phiếu thu thanh toán", "Ghi nhận thanh toán theo đợt", "fa-money-check-dollar"),
                            disabledAction("Sổ hoa hồng", "Tính hoa hồng tự động", "fa-hand-holding-dollar"),
                            disabledAction("Báo cáo tài chính", "Xuất Excel/PDF báo cáo tài chính", "fa-file-export")
                    ))
            );
            case "DIRECTOR" -> List.of(
                    group("Phân tích điều hành", "Theo dõi KPI, doanh thu và hiệu suất doanh nghiệp.", List.of(
                            action("Revenue Summary", "/dashboard/director", "Báo cáo doanh thu, đã thu và còn phải thu", "fa-chart-pie"),
                            action("Executive Dashboard", "/dashboard/director", "KPI overview cho Director", "fa-ranking-star"),
                            disabledAction("Trung tâm xuất báo cáo", "Tải báo cáo Excel/PDF", "fa-file-export")
                    ))
            );
            default -> List.of(
                    group("Khu vực làm việc", "Các chức năng khả dụng theo quyền hiện tại.", List.of(
                            action("Bảng điều khiển", "/dashboard", "Quay về tổng quan", "fa-chart-line"),
                            action("Hồ sơ", "/profile", "Thông tin cá nhân", "fa-user")
                    ))
            );
        };
    }

    private List<Map<String, String>> insightItems(String role) {
        return switch (role) {
            case "ADMIN" -> List.of(
                    insight("Phạm vi dữ liệu: toàn hệ thống", "Quản trị viên quản lý tài khoản, vai trò, phòng ban và cấu hình nền."),
                    insight("Không thao tác bán hàng trực tiếp", "Quản trị viên giám sát hệ thống, không thay thế luồng tác nghiệp của nhân viên kinh doanh.")
            );
            case "MARKETING" -> List.of(
                    insight("Tiếp nhận khách hàng tiềm năng", "Marketing tập trung nhập dữ liệu, kiểm tra trùng và chuyển dữ liệu cho đội kinh doanh."),
                    insight("Không tạo hợp đồng", "Marketing không truy cập pipeline ký kết hoặc contract lifecycle.")
            );
            case "SALES" -> List.of(
                    insight("Phạm vi dữ liệu: cá nhân", "Nhân viên kinh doanh thao tác trên khách hàng, cơ hội và hợp đồng do mình phụ trách."),
                    insight("Hợp đồng từ quy trình bán hàng", "Hợp đồng chỉ được tạo khi cơ hội thắng và báo giá đã duyệt/đã chấp nhận.")
            );
            case "SALES_MANAGER" -> List.of(
                    insight("Phạm vi dữ liệu: nhóm", "Quản lý kinh doanh theo dõi quy trình, khách hàng tiềm năng và KPI của đội."),
                    insight("Không cấu hình phân quyền", "Không truy cập quản lý nhân viên/vai trò.")
            );
            case "ADMIN_OFFICER" -> List.of(
                    insight("Không vào quy trình bán hàng", "Hành chính hợp đồng xử lý hợp đồng chờ thẩm định, không quản lý cơ hội bán hàng."),
                    insight("Rà soát pháp lý", "Có quyền cập nhật điều khoản hợp đồng ở trạng thái chờ thẩm định.")
            );
            case "ACCOUNTANT" -> List.of(
                    insight("Phạm vi tài chính", "Kế toán theo dõi hóa đơn, thanh toán, công nợ và hoa hồng."),
                    insight("Module đang kết nối", "Các màn invoice/payment/report sẽ mở khi phân hệ tài chính hoàn tất.")
            );
            case "DIRECTOR" -> List.of(
                    insight("Góc nhìn điều hành", "Giám đốc tập trung vào KPI doanh thu, hợp đồng và hiệu suất tổng thể."),
                    insight("Bảng điều khiển chỉ đọc", "Không trực tiếp thao tác dữ liệu vận hành hằng ngày.")
            );
            default -> List.of(
                    insight("Bảng điều khiển theo vai trò", "Mỗi người dùng chỉ nhìn thấy khu vực phù hợp với quyền hạn.")
            );
        };
    }

    private String dashboardSubtitle(String role) {
        return switch (role) {
            case "ADMIN" -> "Khởi tạo hệ thống, RBAC, nhân sự và quy tắc nền tảng.";
            case "MARKETING" -> "Tiếp nhận lead, kiểm tra dữ liệu và hỗ trợ phân bổ khách hàng.";
            case "SALES" -> "Chăm sóc khách hàng, quản lý pipeline và hợp đồng cá nhân.";
            case "SALES_MANAGER" -> "Giám sát quy trình bán hàng, phân bổ khách hàng tiềm năng và KPI của đội kinh doanh.";
            case "ADMIN_OFFICER" -> "Rà soát pháp lý, bổ sung điều khoản và xử lý hợp đồng chờ thẩm định.";
            case "ACCOUNTANT" -> "Theo dõi hóa đơn, thanh toán, công nợ và hoa hồng.";
            case "DIRECTOR" -> "Tổng hợp KPI chiến lược và hiệu suất kinh doanh toàn công ty.";
            default -> "Không gian làm việc theo quyền hạn hiện tại.";
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "ADMIN" -> "Quản trị viên";
            case "MARKETING" -> "Nhân viên marketing";
            case "SALES" -> "Nhân viên kinh doanh";
            case "SALES_MANAGER" -> "Quản lý kinh doanh";
            case "ADMIN_OFFICER" -> "Hành chính hợp đồng";
            case "ACCOUNTANT" -> "Kế toán";
            case "DIRECTOR" -> "Giám đốc";
            default -> role;
        };
    }

    private boolean isExecutiveRole(String role) {
        return "DIRECTOR".equals(role);
    }

    private List<Map<String, String>> revenueSummaryRows() {
        long paidInvoices = invoiceRepository.countByStatus(Invoice.InvoiceStatus.PAID);
        long partiallyPaidInvoices = invoiceRepository.countByStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        long issuedInvoices = invoiceRepository.countByStatus(Invoice.InvoiceStatus.ISSUED);
        long unpaidInvoices = issuedInvoices + partiallyPaidInvoices;

        return List.of(
                metric("Doanh thu hợp đồng đã ký", money(contractRepository.sumFinalAmountByStatus(Contract.ContractStatus.SIGNED)), "Tổng final amount của hợp đồng SIGNED", "fa-file-signature", "success"),
                metric("Invoice đã phát hành", money(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.ISSUED)
                        .add(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.PARTIALLY_PAID))
                        .add(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.PAID))), "Tổng giá trị invoice đã gửi/đã thanh toán", "fa-file-invoice-dollar", "info"),
                metric("Đã thu", money(invoiceRepository.sumPaidAmount()), number(paidInvoices) + " invoice đã PAID", "fa-sack-dollar", "success"),
                metric("Còn phải thu", money(invoiceRepository.sumOutstandingAmount()), number(unpaidInvoices) + " invoice chưa tất toán", "fa-money-bill-transfer", "warning")
        );
    }

    private List<Map<String, String>> executiveKpiRows() {
        long won = opportunityRepository.countByStage("WON");
        long lost = opportunityRepository.countByStage("LOST");
        long totalWonLost = won + lost;

        return List.of(
                metric("Tổng hợp đồng", number(contractRepository.count()), "Toàn bộ hợp đồng trong hệ thống", "fa-file-contract", "info"),
                metric("Hợp đồng đã ký", number(contractRepository.countByStatus(Contract.ContractStatus.SIGNED)), "Cơ sở doanh thu đã chốt", "fa-circle-check", "success"),
                metric("Chờ khách ký", number(contractRepository.countByStatus(Contract.ContractStatus.SENT_TO_CUSTOMER)), "Hợp đồng đã gửi khách hàng", "fa-paper-plane", "warning"),
                metric("Tỷ lệ thắng", percent(won, totalWonLost), number(won) + " WON / " + number(totalWonLost) + " WON+LOST", "fa-ranking-star", "success"),
                metric("Khách hàng active", number(customerRepository.countByCustomerStatusIgnoreCase("ACTIVE")), "Hồ sơ khách hàng đang hoạt động", "fa-user-check", "info"),
                metric("Sản phẩm", number(productRepository.count()), "Danh mục sản phẩm/dịch vụ", "fa-boxes-stacked", "warning")
        );
    }

    private Map<String, String> metric(String label, String value, String caption, String icon, String tone) {
        return Map.of("label", label, "value", value, "caption", caption, "icon", icon, "tone", tone);
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

    private String percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0%";
        }
        return String.format("%.1f%%", (numerator * 100.0) / denominator);
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
