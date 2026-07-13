package com.group3.company_management.core.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_code", nullable = false, unique = true, length = 50)
    private String voucherCode;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "is_active")
    private Boolean active;

    @PrePersist
    public void prePersist() {
        if (active == null) {
            active = true;
        }
        if (discountPercent == null) {
            discountPercent = BigDecimal.ZERO;
        }
        if (maxDiscountAmount == null) {
            maxDiscountAmount = BigDecimal.ZERO;
        }
    }
}
