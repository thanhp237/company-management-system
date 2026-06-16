package com.group3.company_management.core.dto;

import com.group3.company_management.core.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime appointmentTime;
    private String status;
    private boolean reminderSent;
    private Long customerId;
    private String customerName; // Hiển thị tên khách hàng ra giao diện cho Sales dễ nhìn

    public static AppointmentResponse fromEntity(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getTitle(),
                appointment.getDescription(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.isReminderSent(),
                appointment.getCustomer().getId(),
                // Lấy tên khách hàng từ thực thể liên kết (Giả sử hàm getName() hoặc getFullName())
                appointment.getCustomer().getFullName() 
        );
    }
}
