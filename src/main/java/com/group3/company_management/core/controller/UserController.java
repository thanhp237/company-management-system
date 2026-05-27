package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private final UserService userService;

    public UserController(UserService service){
        this.userService = service;

    }

    @GetMapping
    public String listUsers(Model model){
        model.addAttribute("users",userService.getAllUsers());
        return "users/list";
    }
    @GetMapping("/add")
    public String showAddForm(Model model){

        model.addAttribute("user",new User());
        return "users/add-form";
    }
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable long id){
        userService.deleteUser(id);
        return "redirect:/users";
    }
    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") User user) {
        userService.createUser(user);
        return "redirect:/users";
    }
}
