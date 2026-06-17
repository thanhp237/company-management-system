package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.service.EmailService;
import com.group3.company_management.core.service.UserService;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";


    @Autowired
    public UserController(UserService service, EmailService emailService){
        this.userService = service;
        this.emailService = emailService;
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
    model.addAttribute("countAccount", userPage.getTotalElements());
    return "users/list";
}

    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long id, Model model) {
        model.addAttribute("userForm", id == null ? new UserRequest() : toRequest(userService.getUserById(id)));
        model.addAttribute("roles", userService.getAllRoles());
        model.addAttribute("isEdit", id != null);
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
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<UserResponse> userPage = userService.searchPage(keyword, status, page, 10);

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
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
            if ("createAndSend".equals(action)) {
                rawPassword = generateTemporaryPassword();
                request.setPassword(rawPassword);
            }

            userService.createUser(request);
            if ("createAndSend".equals(action)) {
                emailService.sendAccountInfo(request.getEmail(), request.getUsername(), rawPassword);
            }
            return "redirect:/users";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("roles", userService.getAllRoles());
            model.addAttribute("isEdit", false);
            return "users/add-form";
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute("userForm") UserRequest request, Model model) {
        try {
            userService.updateUser(request);
            return "redirect:/users";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("roles", userService.getAllRoles());
            model.addAttribute("isEdit", true);
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
        request.setGroupId(response.getGroupId());
        request.setRoleId(response.getRoleId());
        request.setStatus(response.getStatus());
        return request;
    }
}
