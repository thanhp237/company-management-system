package com.group3.company_management.core.controller;


import com.group3.company_management.core.dto.UserRequest;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.service.DepartmentService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;


    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }
    @GetMapping
    public String listDepartments(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<Department> departmentPage = departmentService.getDepartmentsPage(page);

        model.addAttribute("departmentPage", departmentPage);

        return "departments/list";
    }
    @PostMapping("/update-status")
    public String updateStatus(@RequestParam Long id,
                               @RequestParam String status) {
        departmentService.updateDepartmentStatus(id,status);
        return "redirect:/departments";
    }



    @PostMapping("/save")
    public String saveDepartment( @ModelAttribute("department") Department department ,Model model) {
      try{
          departmentService.saveDepartment(department);
          return "redirect:/departments";
      }catch (RuntimeException e){
          model.addAttribute("err", e.getMessage());
          return "departments/form";
      }


    }

    @GetMapping("/edit")
    public String showEditForm(@RequestParam(required = false) Long id, Model model) {
        Department department;
       if(id == null){
            department = new Department();
       }else{
           department = departmentService.getDepartmentById(id);
       }

        model.addAttribute("department", department);

        return "departments/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return "redirect:/departments";
    }
    @GetMapping("/search")
       public String searchByNameAndId(@RequestParam("keyword") String keyword,Model model){
        try{
            List<Department> listSearch = departmentService.searchByIdandName(keyword);
            model.addAttribute("departments", listSearch);
            return "departments/list";

        }catch (RuntimeException e){
            model.addAttribute("err", e.getMessage());
            model.addAttribute("departments",
                    departmentService.getAllDepartments());
            return "departments/list";
        }
        }
    }
