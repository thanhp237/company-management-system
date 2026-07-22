package com.group3.company_management.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_targets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_targets_sale_period",
                columnNames = {"sale_employee_id", "target_year", "target_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_employee_id", nullable = false)
    private Employee sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    @Column(name = "target_month", nullable = false)
    private Integer targetMonth;

    @Builder.Default
    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "bonus_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal bonusRate = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (targetAmount == null) {
            targetAmount = BigDecimal.ZERO;
        }
        if (bonusRate == null) {
            bonusRate = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (targetAmount == null) {
            targetAmount = BigDecimal.ZERO;
        }
        if (bonusRate == null) {
            bonusRate = BigDecimal.ZERO;
        }
    }
}
