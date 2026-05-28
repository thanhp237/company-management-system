package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService service){
        this.userService = service;
    }

    @GetMapping
    public String listUsers(Model model){
        model.addAttribute("users", userService.getAllUsers());
        return "users/list";
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long id, Model model){
        model.addAttribute("userForm", id == null ? new UserRequest() : toRequest(userService.getUserById(id)));
        model.addAttribute("roles", userService.getAllRoles());
        model.addAttribute("isEdit", id != null);
    public String showAddForm(Model model){
        model.addAttribute("userForm", new UserRequest());
        model.addAttribute("roles", userService.getAllRoles());
        return "users/add-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable long id){
        userService.deleteUser(id);
        return "redirect:/users";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("userForm") UserRequest request, Model model) {
        try {
            userService.createUser(request);
            return "redirect:/users";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("roles", userService.getAllRoles());
            model.addAttribute("isEdit", false);
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
