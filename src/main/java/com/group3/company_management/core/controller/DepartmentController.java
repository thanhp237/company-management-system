package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.service.DepartmentService;
import com.group3.company_management.core.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/departments")
public class DepartmentController {
    private final UserService userService;
    private final DepartmentService departmentService;

    public DepartmentController(UserService userService, DepartmentService departmentService) {
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','SALES_MANAGER','SALES','ACCOUNTANT','MARKETING')")
    public String listDepartments(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Department> departmentPage = departmentService.getDepartmentsPage(page);

        model.addAttribute("departmentPage", departmentPage);
        model.addAttribute("keyword", "");
        model.addAttribute("status", "all");
        addDepartmentSummary(model, departmentPage);

        return "departments/list";
    }

    @GetMapping("/edit")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
    public String showEditForm(@RequestParam(required = false) Long id, Model model) {
        Department department = id == null
                ? new Department()
                : departmentService.getDepartmentById(id);

        model.addAttribute("department", department);
        model.addAttribute("currentMembers", id == null ? 0 : departmentService.countMembers(id));
        model.addAttribute("assignableUsers", departmentService.getAssignableUsers(id));
        model.addAttribute("managers", userService.getUsersByRoles(
                List.of("ADMIN_OFFICER", "SALES_MANAGER")
        ));

        return "departments/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
    public String saveDepartment(@ModelAttribute("department") Department department,
                                 @RequestParam(required = false) List<Long> userIds,
                                 Model model) {
        try {
            Department savedDepartment = departmentService.saveDepartment(department);
            departmentService.addUsersToDepartment(userIds, savedDepartment.getId());

            return "redirect:/departments";
        } catch (RuntimeException e) {
            Long id = department.getId();
            model.addAttribute("err", e.getMessage());
            model.addAttribute("department", department);
            model.addAttribute("currentMembers", id == null ? 0 : departmentService.countMembers(id));
            model.addAttribute("assignableUsers", departmentService.getAssignableUsers(id));
            model.addAttribute("managers", userService.getUsersByRoles(
                    List.of("ADMIN_OFFICER", "SALES_MANAGER")
            ));

            return "departments/form";
        }
    }

    @PostMapping("/assign-user")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
    public String assignUser(@RequestParam List<Long> userIds,
                             @RequestParam Long departmentId,
                             RedirectAttributes redirectAttributes) {
        try {
            departmentService.addUsersToDepartment(userIds, departmentId);
            redirectAttributes.addFlashAttribute("success", "Gán nhân viên vào phòng ban thành công");
        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute("err", e.getMessage());
        }

        return "redirect:/departments/edit?id=" + departmentId;
    }

    @PostMapping("/update-status")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
    public String updateStatus(@RequestParam Long id, @RequestParam String status) {
        departmentService.updateDepartmentStatus(id, status);
        return "redirect:/departments";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDepartment(@PathVariable Long id, Authentication authentication) {
        departmentService.deleteDepartment(id, authentication.getName());
        return "redirect:/departments";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','SALES_MANAGER','SALES','ACCOUNTANT','MARKETING')")
    public String searchByNameAndId(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<Department> listSearch = departmentService.searchByIdandName(keyword, status, page);

        model.addAttribute("departmentPage", listSearch);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        addDepartmentSummary(model, listSearch);

        return "departments/list";
    }

    private void addDepartmentSummary(Model model, Page<Department> departmentPage) {
        List<Department> departments = departmentPage.getContent();

        long activeCount = departments.stream()
                .filter(department -> "ACTIVE".equalsIgnoreCase(department.getStatus()))
                .count();

        long inactiveCount = departments.size() - activeCount;

        Map<Long, String> managerMap = userService.getUsersByRoles(List.of("ADMIN_OFFICER", "SALES_MANAGER"))
                .stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getFullName() != null && !user.getFullName().isBlank()
                                ? user.getFullName()
                                : user.getUsername(),
                        (first, second) -> first
                ));

        Map<Long, Integer> memberCountMap = departmentService.countMembersByDepartmentIds(
                departments.stream().map(Department::getId).toList()
        );

        model.addAttribute("totalDepartments", departmentPage.getTotalElements());
        model.addAttribute("activeDepartments", activeCount);
        model.addAttribute("inactiveDepartments", inactiveCount);
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("managerMap", managerMap);
        model.addAttribute("memberCountMap", memberCountMap);
    }
}