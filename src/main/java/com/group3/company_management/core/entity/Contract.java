package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "contract_code", nullable = false, unique = true, length = 50)
    private String contractCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 30)
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "contract_amount", precision = 15, scale = 2)
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "final_amount", precision = 15, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee")
    private Employee assignedEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Employee sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_officer_id")
    private Employee adminOfficer;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "delivery_terms", columnDefinition = "TEXT")
    private String deliveryTerms;

    @Column(name = "legal_terms", columnDefinition = "TEXT")
    private String legalTerms;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (contractAmount == null) {
            contractAmount = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (finalAmount == null) {
            finalAmount = BigDecimal.ZERO;
        }
    }

    public enum ContractStatus {
        DRAFT,
        PENDING_ADMIN_OFFICER,
        ADMIN_REVIEWED,
        SENT_TO_CUSTOMER,
        SIGNED,
        CANCELLED
    }

}
