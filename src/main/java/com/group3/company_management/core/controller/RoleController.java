package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.RoleSummaryDTO;
import com.group3.company_management.core.entity.Role;
import com.group3.company_management.core.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public String listRoles(Model model) {
        List<RoleSummaryDTO> roles = roleService.getRoleSummaries();
        model.addAttribute("roles", roles);
        model.addAttribute("totalRoles", roles.size());
        model.addAttribute("activeRoles", roles.stream().filter(role -> "ACTIVE".equals(role.getStatus())).count());
        model.addAttribute("inactiveRoles", roles.stream().filter(role -> "INACTIVE".equals(role.getStatus())).count());
        model.addAttribute("assignedUsers", roles.stream().mapToLong(RoleSummaryDTO::getTotalCount).sum());
        return "roles/list";
    }

    @GetMapping("/edit")
    public String showRoleForm(@RequestParam(required = false) Long id, Model model) {
        Role role = roleService.getRoleForm(id);
        model.addAttribute("role", role);
        model.addAttribute("isEdit", id != null);
        model.addAttribute("isSystemRole", id != null && roleService.isSystemRole(role.getRoleCode()));
        return "roles/form";
    }

    @PostMapping("/save")
    public String saveRole(
            @ModelAttribute("role") Role role,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            roleService.saveRole(role);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu vai trò.");
            return "redirect:/roles";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("isEdit", role.getId() != null);
            model.addAttribute("isSystemRole", role.getId() != null && roleService.isSystemRole(role.getRoleCode()));
            return "roles/form";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            roleService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái vai trò.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/roles";
    }

    @PostMapping("/{id}/delete")
    public String deleteRole(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            roleService.deleteRole(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vai trò.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/roles";
    }
}
