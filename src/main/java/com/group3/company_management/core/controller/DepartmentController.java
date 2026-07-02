package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.DepartmentRequest;
import com.group3.company_management.core.dto.DepartmentResponse;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.service.DepartmentService;
import com.group3.company_management.core.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    public String getListDepartment(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10") int size,
            Model model){
        Pageable  pageable = PageRequest.of(page,size, Sort.by("id").ascending());
        Page<DepartmentResponse> departmentPage = departmentService.search(keyword, status, pageable);

        model.addAttribute("departments", departmentPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "departments/list";
    }
    @GetMapping("/edit")
    public String editDepartment(@RequestParam(required = false) Long id,Model model){
        DepartmentRequest department;
        if(id == null){
            department = new DepartmentRequest();
        }else{
             department= departmentService.getDepartmentById(id);
        }
        model.addAttribute("department", department);
        model.addAttribute("listEmployee", userService.getUsersByRoles(List.of("SALES","ACCOUNTANT","MARKETING")));
        model.addAttribute(("listManager"), userService.getUsersByRoles(List.of("ADMIN","ADMIN_OFFICER","SALES_MANAGER")));
        return "departments/form";
    }
    @PostMapping("/save")
    public String saveDepartment(@ModelAttribute("department") DepartmentRequest departmentRequest,
                                 Authentication authentication,
                                 Model model) {
        try {
            departmentService.saveDepartment(departmentRequest, authentication.getName());
            return "redirect:/departments";
        } catch (RuntimeException e) {
            model.addAttribute("err", e.getMessage());
            model.addAttribute("department", departmentRequest);
            model.addAttribute("listEmployee",
                    userService.getUsersByRoles(List.of("SALES","ACCOUNTANT","MARKETING")));

            model.addAttribute("listManager", userService.getUsersByRoles(List.of("ADMIN","ADMIN_OFFICER","SALES_MANAGER")));
            return "departments/form";
        }
    }
    @GetMapping("/delete")
    public String deleteDepartment(@RequestParam Long id,
                                   Authentication authentication) {

        departmentService.deleteDepartment(id, authentication.getName());

        return "redirect:/departments";
    }
    @PostMapping("/active")
    public String updateStatus(@RequestParam String status,@RequestParam Long id,
                               Model model){
        departmentService.updateDepartmentStatus(id,status);
        return "redirect:/departments";
    }
    @GetMapping("/detail")
    public String detailDepartment(@RequestParam Long id, Model model){

        DepartmentRequest department = departmentService.getDepartmentById(id);

        model.addAttribute("department", department);

        return "departments/detail";
    }































































//    @GetMapping
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','SALES_MANAGER','SALES','ACCOUNTANT','MARKETING')")
//    public String listDepartments(@RequestParam(defaultValue = "0") int page, Model model) {
//        Page<Department> departmentPage = departmentService.getDepartmentsPage(page);
//
//        model.addAttribute("departmentPage", departmentPage);
//        model.addAttribute("keyword", "");
//        model.addAttribute("status", "all");
//        addDepartmentSummary(model, departmentPage);
//
//        return "departments/list";
//    }
//
//    @GetMapping("/edit")
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
//    public String showEditForm(@RequestParam(required = false) Long id, Model model) {
//        Department department = id == null
//                ? new Department()
//                : departmentService.getDepartmentById(id);
//
//        model.addAttribute("department", department);
//        model.addAttribute("currentMembers", id == null ? 0 : departmentService.countMembers(id));
//        model.addAttribute("assignableUsers", departmentService.getAssignableUsers(id));
//        model.addAttribute("managers", userService.getUsersByRoles(
//                List.of("ADMIN", "SALES_MANAGER")
//                //  userService.getUserByRoles
//        ));
//
//        return "departments/form";
//    }
//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
//    public String saveDepartment(@ModelAttribute("department") Department department,
//                                 @RequestParam(required = false) List<Long> userIds,
//                                 Model model) {
//        try {
//            Department savedDepartment = departmentService.saveDepartment(department);
//            departmentService.addUsersToDepartment(userIds, savedDepartment.getId());
//
//            return "redirect:/departments";
//        } catch (RuntimeException e) {
//            Long id = department.getId();
//            model.addAttribute("err", e.getMessage());
//            model.addAttribute("department", department);
//            model.addAttribute("currentMembers", id == null ? 0 : departmentService.countMembers(id));
//            model.addAttribute("assignableUsers", departmentService.getAssignableUsers(id));
//            model.addAttribute("managers", userService.getUsersByRoles(
//                    List.of("ADMIN", "SALES_MANAGER")
//            ));
//
//
//            return "departments/form";
//        }
//    }
//
//    @PostMapping("/assign-user")
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
//    public String assignUser(@RequestParam List<Long> userIds,
//                             @RequestParam Long departmentId,
//                             RedirectAttributes redirectAttributes) {
//        try {
//            departmentService.addUsersToDepartment(userIds, departmentId);
//            redirectAttributes.addFlashAttribute("success", "Gán nhân viên vào phòng ban thành công");
//        } catch (RuntimeException e) {
//
//            redirectAttributes.addFlashAttribute("err", e.getMessage());
//        }
//
//        return "redirect:/departments/edit?id=" + departmentId;
//    }
//
//    @PostMapping("/update-status")
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
//    public String updateStatus(@RequestParam Long id, @RequestParam String status) {
//        departmentService.updateDepartmentStatus(id, status);
//        return "redirect:/departments";
//    }
//
//    @GetMapping("/delete/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public String deleteDepartment(@PathVariable Long id, Authentication authentication) {
//        departmentService.deleteDepartment(id, authentication.getName());
//        return "redirect:/departments";
//    }
//
//    @GetMapping("/search")
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','SALES_MANAGER','SALES','ACCOUNTANT','MARKETING')")
//    public String searchByNameAndId(
//            @RequestParam(defaultValue = "") String keyword,
//            @RequestParam(defaultValue = "all") String status,
//            @RequestParam(defaultValue = "0") int page,
//            Model model) {
//
//        Page<Department> listSearch = departmentService.searchByIdandName(keyword, status, page);
//
//        model.addAttribute("departmentPage", listSearch);
//        model.addAttribute("keyword", keyword);
//        model.addAttribute("status", status);
//        addDepartmentSummary(model, listSearch);
//
//        return "departments/list";
//    }
//    @GetMapping("/detail/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','SALES_MANAGER','SALES','ACCOUNTANT','MARKETING')")
//    public String detailDepartment(@PathVariable Long id, Model model) {
//        Department department = departmentService.getDepartmentById(id);
//
//        model.addAttribute("department", department);
//        model.addAttribute("currentMembers", departmentService.countMembers(id));
//        model.addAttribute("members", departmentService.getUsersInDepartment(id));
//        model.addAttribute("manager", departmentService.getDepartmentManager(department.getManagerId()));
//
//        return "departments/detail";
//    }
//
//    private void addDepartmentSummary(Model model, Page<Department> departmentPage) {
//
//        List<Department> departments = departmentPage.getContent();
//
//
//        int activeCount = 0;
//        int inactiveCount = 0;
//
//        for (Department department : departments) {
//            if ("ACTIVE".equalsIgnoreCase(department.getStatus())) {
//                activeCount++;
//            } else {
//                inactiveCount++;
//            }
//        }
//
//
//        List<User> managers = userService.getUsersByRoles(
//                List.of("ADMIN", "SALES_MANAGER")
//        );
//        Map<Long, String> managerMap = new HashMap<>();
//
//        for (User manager : managers) {
//
//            String managerName = manager.getFullName();
//
//            if (managerName == null || managerName.isBlank()) {
//                managerName = manager.getUsername();
//            }
//
//            managerMap.put(manager.getId(), managerName);
//        }
//
//
//        List<Long> departmentIds = new ArrayList<>();
//
//        for (Department department : departments) {
//            departmentIds.add(department.getId());
//        }
//
//
//        Map<Long, Integer> memberCountMap =
//                departmentService.countMembersByDepartmentIds(departmentIds);
//
//
//        model.addAttribute("totalDepartments", departmentPage.getTotalElements());
//        model.addAttribute("activeDepartments", activeCount);
//        model.addAttribute("inactiveDepartments", inactiveCount);
//        model.addAttribute("totalUsers", userService.countUsers());
//        model.addAttribute("managerMap", managerMap);
//        model.addAttribute("memberCountMap", memberCountMap);
//
//    }
}