package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.DepartmentDTO;
import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.service.DepartmentService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("department", new DepartmentDTO());
        return "departments/form";
    }

    @PostMapping("/save")
    public String saveDepartment(@ModelAttribute("department") DepartmentDTO departmentdto) {
        Department department;
        if(departmentdto.getId()!= null){
            department = departmentService.getDepartmentById(departmentdto.getId());
        }else {
            department  = new Department();
        }
        department.setCode(departmentdto.getCode());
        department.setName(departmentdto.getName());
        departmentService.saveDepartment(department);
        return "redirect:/departments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {

        Department department = departmentService.getDepartmentById(id);

        DepartmentDTO dto = new DepartmentDTO(
                department.getId(),
                department.getCode(),
                department.getName()
        );

        model.addAttribute("department", dto);

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
