package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.AppointmentRequest;
import com.group3.company_management.core.dto.AppointmentResponse;
import com.group3.company_management.core.service.AppointmentService;
import com.group3.company_management.core.repository.CustomerRepository; // Nhúng vào để lấy danh sách khách hàng cho ô Dropdown
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CustomerRepository customerRepository; // Sử dụng để đổ danh sách khách hàng vào Form tạo lịch hẹn

    // 1. Hiển thị danh sách lịch hẹn của riêng nhân viên đang đăng nhập
    @GetMapping
    public String listAppointments(Principal principal, Model model) {
        String username = principal.getName();
        List<AppointmentResponse> appointments = appointmentService.getAppointmentsByEmployee(username);
        
        model.addAttribute("appointments", appointments);
        return "appointments/list";
    }

    // 2. Trả về giao diện Form thêm mới lịch hẹn
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("appointmentRequest", new AppointmentRequest());
        // Lấy toàn bộ danh sách khách hàng để nhân viên Sales chọn trong thẻ <select> trên giao diện
        model.addAttribute("customers", customerRepository.findAll()); 
        return "appointments/form";
    }

    // 3. Xử lý lưu lịch hẹn mới từ Form gửi lên
    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute AppointmentRequest request, Principal principal) {
        String username = principal.getName();
        appointmentService.createAppointment(request, username);
        return "redirect:/appointments?success";
    }

    // 4. Xử lý cập nhật trạng thái lịch hẹn (Ví dụ: Đổi sang COMPLETED hoặc CANCELLED)
    @PostMapping("/update-status")
    public String updateStatus(@RequestParam("id") Long id, @RequestParam("status") String status) {
        appointmentService.updateStatus(id, status);
        return "redirect:/appointments?statusUpdated";
    }
}