package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_schedules", uniqueConstraints =
@UniqueConstraint(columnNames = {"contract_id", "installment_no"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    private String description;
}
