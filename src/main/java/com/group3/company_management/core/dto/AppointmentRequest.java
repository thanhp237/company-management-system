package com.group3.company_management.core.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    private String title;
    private String description;
    private LocalDateTime appointmentTime;
    private Long customerId; // Chọn ID khách hàng từ danh sách thả xuống (Dropdown)
}