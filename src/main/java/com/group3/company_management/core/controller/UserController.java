package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.service.DepartmentService;
import com.group3.company_management.core.service.EmailService;
import com.group3.company_management.core.service.UserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final DepartmentService departmentService;


    @Autowired
    public UserController(UserService service, EmailService emailService, DepartmentService departmentService){
        this.userService = service;
        this.emailService = emailService;
        this.departmentService = departmentService;
    }

@GetMapping
public String listUsers(
        @RequestParam(required = false) String role,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
    Page<UserResponse> userPage = userService.getUsersPage(role, page, 10);

    model.addAttribute("userPage", userPage);
    model.addAttribute("users", userPage.getContent());
    model.addAttribute("role", role);
    model.addAttribute("roles", userService.getAllRoles());
    model.addAttribute("countAccount", userPage.getTotalElements());
    return "users/list";
}

    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long id, Model model) {
        prepareUserFormModel(model, id == null ? new UserRequest() : toRequest(userService.getUserById(id)), id != null);
        return "users/add-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable long id){
        userService.deleteUser(id);
        return "redirect:/users";
    }
    @GetMapping("/find")
    public String findUser(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<UserResponse> userPage = userService.searchPage(keyword, status, role, page, 10);

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("role", role);
        model.addAttribute("roles", userService.getAllRoles());
        model.addAttribute("isSearch", true);
        model.addAttribute("countAccount", userPage.getTotalElements());

        return "users/list";
    }
    @PostMapping("/save")
    public String saveUser(
            @ModelAttribute("userForm") UserRequest request,
            @RequestParam(defaultValue = "create") String action,
            Model model) {
        try {
            String rawPassword = request.getPassword();
            userService.createUser(request);
            if ("createAndSend".equals(action)) {
                emailService.sendAccountInfo(request.getEmail(), request.getUsername(), rawPassword);
            }
            return "redirect:/users";
        } catch (MailException exception) {
            request.setPassword(null);
            model.addAttribute("errorMessage", "Account created, but email could not be sent. Please check Gmail SMTP configuration.");
            prepareUserFormModel(model, request, false);
            return "users/add-form";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            prepareUserFormModel(model, request, false);
            return "users/add-form";
        } catch (Exception exception) {
            request.setPassword(null);
            model.addAttribute("errorMessage", "Could not create account: " + exception.getMessage());
            prepareUserFormModel(model, request, false);
            return "users/add-form";
        }
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute("userForm") UserRequest request, Model model) {
        try {
            userService.updateUser(request);
            return "redirect:/users";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            prepareUserFormModel(model, request, true);
            return "users/add-form";
        }
    }

    @PostMapping("/update-status")
    public String updateStatus(@ModelAttribute UserRequest request) {
        userService.updateUserStatus(request);
        return "redirect:/users";
    }

    private UserRequest toRequest(UserResponse response) {
        UserRequest request = new UserRequest();
        request.setId(response.getId());
        request.setUsername(response.getUsername());
        request.setEmail(response.getEmail());
        request.setFullName(response.getFullName());
        request.setPhone(response.getPhone());
        request.setDepartmentId(response.getDepartmentId());
        request.setRoleId(response.getRoleId());
        request.setStatus(response.getStatus());
        return request;
    }

    private void prepareUserFormModel(Model model, UserRequest request, boolean isEdit) {
        model.addAttribute("userForm", request);
        model.addAttribute("roles", userService.getAllRoles());
        
        List<com.group3.company_management.core.entity.Department> allDepts = departmentService.getAllDepartments();
        if (isEdit && request.getDepartmentId() != null) {
            // Đảm bảo phòng ban hiện tại của nhân viên được sửa vẫn hiển thị kể cả khi nó đang tạm khóa/không hoạt động
            model.addAttribute("departments", allDepts.stream()
                    .filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus()) || request.getDepartmentId().equals(d.getId()))
                    .toList());
        } else {
            model.addAttribute("departments", allDepts.stream()
                    .filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus()))
                    .toList());
        }
        model.addAttribute("isEdit", isEdit);
    }
}
