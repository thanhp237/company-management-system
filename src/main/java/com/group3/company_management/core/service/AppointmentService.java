package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.AppointmentRequest;
import com.group3.company_management.core.dto.AppointmentResponse;
import java.util.List;

public interface AppointmentService {
    // 1. Lấy tất cả lịch hẹn của nhân viên đang đăng nhập
    List<AppointmentResponse> getAppointmentsByEmployee(String username);
    
    // 2. Tạo mới lịch hẹn
    void createAppointment(AppointmentRequest request, String username);
    
    // 3. Thay đổi trạng thái lịch hẹn (COMPLETED hoặc CANCELLED)
    void updateStatus(Long id, String status);
    
    // 4. Quét hệ thống để tìm các lịch hẹn sắp diễn ra trong vòng 24 giờ tới (phục vụ tính năng thông báo)
    List<AppointmentResponse> checkAndGetUpcomingReminders();
}