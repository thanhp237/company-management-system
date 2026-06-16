package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent = false;

    @Column(nullable = false, length = 30)
    private String status; // PENDING, COMPLETED, CANCELLED

    // Liên kết với tài khoản nhân viên (employee_id trong DB chính là User ID hệ thống)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    // Giả sử nhóm bạn đã có entity Customer, ta map khóa ngoại customer_id vào đây.
    // Nếu chưa có entity Customer, bạn có thể tạm thay bằng: private Long customerId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}