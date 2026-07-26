package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="payment_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Invoice invoice;
    @Column(name="txn_ref", unique=true, nullable=false) private String txnRef;
    @Column(nullable=false, precision=18, scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Builder.Default private Status status=Status.PENDING;
    private String vnpTransactionNo;
    private String responseCode;
    @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime completedAt;
    public enum Status { PENDING, SUCCESS_PENDING_REVIEW, SUCCESS, FAILED }
}

