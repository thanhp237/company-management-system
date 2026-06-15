package com.group3.company_management.core.controller;


import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.service.DepartmentService;

import jakarta.validation.Valid;
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
    public String listDepartments(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "departments/list";
    }



    @PostMapping("/save")
    public String saveDepartment(@Valid @ModelAttribute("department") Department department, BindingResult bindingResult) {
      if(bindingResult.hasErrors()){
          return "departments/form";
      }
        departmentService.saveDepartment(department);
        return "redirect:/departments";
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
        List<Department> listSearch = departmentService.searchByIdandName(keyword);
        model.addAttribute("departments", listSearch);
        return "departments/list";
        }
    }
