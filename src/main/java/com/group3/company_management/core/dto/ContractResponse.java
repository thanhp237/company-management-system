package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ContractResponse {

    private Long id;

    private String contractCode;

    private String status;

    private Long quotationId;

    private String quotationCode;

    private Long customerId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String customerAddress;

    private Boolean customerAccountCreated;

    private BigDecimal contractAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Long voucherId;

    private String voucherCode;

    private BigDecimal voucherDiscountPercent;

    private BigDecimal voucherMaxDiscountAmount;

    private Long saleId;

    private String saleName;

    private Long adminOfficerId;

    private String adminOfficerName;

    private LocalDateTime createdAt;

    private LocalDateTime signedAt;

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    private String paymentTerms;
    private String paymentPlanType;
    private String revisionReason;
    private List<PaymentScheduleResponse> paymentSchedules;

    private String deliveryTerms;

    private String legalTerms;

    private String adminNote;

    private LocalDate signingDate;

    private String signingPlace;

    private String sellerCompanyName;

    private String sellerTaxCode;

    private String sellerAddress;

    private String sellerPhone;

    private String sellerFax;

    private String sellerBankAccount;

    private String sellerBankName;

    private String sellerRepresentativeName;

    private String sellerRepresentativeTitle;

    private String sellerIdentityNumber;

    private String sellerIdentityIssuedPlace;

    private LocalDate sellerIdentityIssuedDate;

    private String sellerAuthorizationInfo;

    private String buyerCompanyName;

    private String buyerTaxCode;

    private String buyerAddress;

    private String buyerPhone;

    private String buyerFax;

    private String buyerBankAccount;

    private String buyerBankName;

    private String buyerRepresentativeName;

    private String buyerRepresentativeTitle;

    private String buyerIdentityNumber;

    private String buyerIdentityIssuedPlace;

    private LocalDate buyerIdentityIssuedDate;

    private String buyerAuthorizationInfo;

    private String amountInWords;

    private LocalDate paymentDueDate;

    private String paymentMethod;

    private String deliverySchedule;

    private String shippingResponsibility;

    private String unloadingCost;

    private String storageFeePerDay;

    private String inspectionAgency;

    private String warrantyProductScope;

    private Integer warrantyMonths;

    private String penaltyRate;

    private Integer contractCopies;

    private Integer copiesPerParty;

    private String generalTerms;

    private List<ContractItemResponse> items;
}
