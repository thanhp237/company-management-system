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

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_plan_type", length = 20)
    private PaymentPlanType paymentPlanType;

    @Column(name = "revision_reason", columnDefinition = "TEXT")
    private String revisionReason;

    @Column(name = "delivery_terms", columnDefinition = "TEXT")
    private String deliveryTerms;

    @Column(name = "legal_terms", columnDefinition = "TEXT")
    private String legalTerms;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "signing_date")
    private LocalDate signingDate;

    @Column(name = "signing_place", length = 255)
    private String signingPlace;

    @Column(name = "seller_company_name", length = 255)
    private String sellerCompanyName;

    @Column(name = "seller_tax_code", length = 100)
    private String sellerTaxCode;

    @Column(name = "seller_address", columnDefinition = "TEXT")
    private String sellerAddress;

    @Column(name = "seller_phone", length = 50)
    private String sellerPhone;

    @Column(name = "seller_fax", length = 50)
    private String sellerFax;

    @Column(name = "seller_bank_account", length = 100)
    private String sellerBankAccount;

    @Column(name = "seller_bank_name", length = 255)
    private String sellerBankName;

    @Column(name = "seller_representative_name", length = 255)
    private String sellerRepresentativeName;

    @Column(name = "seller_representative_title", length = 100)
    private String sellerRepresentativeTitle;

    @Column(name = "seller_identity_number", length = 50)
    private String sellerIdentityNumber;

    @Column(name = "seller_identity_issued_place", length = 255)
    private String sellerIdentityIssuedPlace;

    @Column(name = "seller_identity_issued_date")
    private LocalDate sellerIdentityIssuedDate;

    @Column(name = "seller_authorization_info", columnDefinition = "TEXT")
    private String sellerAuthorizationInfo;

    @Column(name = "buyer_company_name", length = 255)
    private String buyerCompanyName;

    @Column(name = "buyer_tax_code", length = 100)
    private String buyerTaxCode;

    @Column(name = "buyer_address", columnDefinition = "TEXT")
    private String buyerAddress;

    @Column(name = "buyer_phone", length = 50)
    private String buyerPhone;

    @Column(name = "buyer_fax", length = 50)
    private String buyerFax;

    @Column(name = "buyer_bank_account", length = 100)
    private String buyerBankAccount;

    @Column(name = "buyer_bank_name", length = 255)
    private String buyerBankName;

    @Column(name = "buyer_representative_name", length = 255)
    private String buyerRepresentativeName;

    @Column(name = "buyer_representative_title", length = 100)
    private String buyerRepresentativeTitle;

    @Column(name = "buyer_identity_number", length = 50)
    private String buyerIdentityNumber;

    @Column(name = "buyer_identity_issued_place", length = 255)
    private String buyerIdentityIssuedPlace;

    @Column(name = "buyer_identity_issued_date")
    private LocalDate buyerIdentityIssuedDate;

    @Column(name = "buyer_authorization_info", columnDefinition = "TEXT")
    private String buyerAuthorizationInfo;

    @Column(name = "amount_in_words", columnDefinition = "TEXT")
    private String amountInWords;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    @Column(name = "payment_method", length = 255)
    private String paymentMethod;

    @Column(name = "delivery_schedule", columnDefinition = "TEXT")
    private String deliverySchedule;

    @Column(name = "shipping_responsibility", length = 255)
    private String shippingResponsibility;

    @Column(name = "unloading_cost", length = 255)
    private String unloadingCost;

    @Column(name = "storage_fee_per_day", length = 255)
    private String storageFeePerDay;

    @Column(name = "inspection_agency", length = 255)
    private String inspectionAgency;

    @Column(name = "warranty_product_scope", columnDefinition = "TEXT")
    private String warrantyProductScope;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "penalty_rate", length = 50)
    private String penaltyRate;

    @Column(name = "contract_copies")
    private Integer contractCopies;

    @Column(name = "copies_per_party")
    private Integer copiesPerParty;

    @Column(name = "general_terms", columnDefinition = "TEXT")
    private String generalTerms;

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
        REVISION_REQUESTED,
        SIGNED,
        CANCELLED
    }

    public enum PaymentPlanType { ONE_TIME, INSTALLMENTS }

}
