package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.RoleSummaryDTO;
import com.group3.company_management.core.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public String listRoles(Model model) {
        List<RoleSummaryDTO> roles = roleService.getRoleSummaries();
        model.addAttribute("roles", roles);
        return "roles/list"; // Đường dẫn tới file HTML: templates/roles/list.html
    }
}