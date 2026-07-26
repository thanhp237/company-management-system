package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.DepartmentRequest;
import com.group3.company_management.core.dto.DepartmentResponse;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
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
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/departments")
public class DepartmentController {
    private final UserService userService;
    private final DepartmentService departmentService;
   private UserRepository userRepository;

    public DepartmentController(UserService userService, DepartmentService departmentService, UserRepository userRepository) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_OFFICER', 'MANAGER', 'SALES_MANAGER')")
    public String getListDepartment(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<DepartmentResponse> departmentPage = departmentService.search(keyword, status, pageable);

        model.addAttribute("departments", departmentPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "departments/list";
    }
    @GetMapping("/edit")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String editDepartment(@RequestParam(required = false) Long id,Model model){
        DepartmentRequest department;
        if(id == null){
            department = new DepartmentRequest();
        }else{
             department= departmentService.getDepartmentById(id);
        }
        model.addAttribute("department", department);
        if(id == null){
            model.addAttribute(
                    "listEmployee",
                    userRepository.findAvailableEmployees(
                            List.of("SALES","ACCOUNTANT","MARKETING")
                    )
            );
        }else{
            model.addAttribute(
                    "listEmployee",
                    userRepository.findAvailableEmployeesForEdit(
                            id,
                            List.of("SALES","ACCOUNTANT","MARKETING")
                    )
            );
        }
        model.addAttribute(("listManager"), userService.getUsersByRoles(List.of("ADMIN","ADMIN_OFFICER","SALES_MANAGER","MANAGER")));
        return "departments/form";
    }
    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String saveDepartment(@ModelAttribute("department") DepartmentRequest departmentRequest,
                                 Authentication authentication,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            departmentService.saveDepartment(departmentRequest, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    departmentRequest.getId() == null ? "Thêm phòng ban mới thành công!" : "Cập nhật thông tin phòng ban thành công!");
            return "redirect:/departments";
        } catch (RuntimeException e) {
            model.addAttribute("err", e.getMessage());
            model.addAttribute("department", departmentRequest);
            if (departmentRequest.getId() == null) {
                model.addAttribute("listEmployee",
                        userRepository.findAvailableEmployees(List.of("SALES", "ACCOUNTANT", "MARKETING")));
            } else {
                model.addAttribute("listEmployee",
                        userRepository.findAvailableEmployeesForEdit(departmentRequest.getId(), List.of("SALES", "ACCOUNTANT", "MARKETING")));
            }

            model.addAttribute("listManager", userService.getUsersByRoles(List.of("ADMIN","ADMIN_OFFICER","SALES_MANAGER","MANAGER")));
            return "departments/form";
        }
    }
    @GetMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String deleteDepartment(@RequestParam Long id,
                                   Authentication authentication) {

        departmentService.deleteDepartment(id, authentication.getName());

        return "redirect:/departments";
    }
    @PostMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String updateStatus(@RequestParam String status,@RequestParam Long id,
                               Model model){
        departmentService.updateDepartmentStatus(id,status);
        return "redirect:/departments";
    }
    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String detailDepartment(@RequestParam Long id, Model model){

        DepartmentRequest department = departmentService.getDepartmentById(id);

        model.addAttribute("department", department);

        return "departments/detail";
    }






}