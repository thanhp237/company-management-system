package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private Long accountId; // ID tài khoản nhận thông báo (liên kết với system_accounts)

    @Column(name = "customer_id")
    private Long customerId; // ID khách hàng nhận thông báo (liên kết với customers)

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}