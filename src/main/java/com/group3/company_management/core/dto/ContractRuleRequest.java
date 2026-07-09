package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ContractRuleRequest {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractEndDate;

    private String paymentTerms;
    private String paymentPlanType;
    private List<PaymentScheduleRequest> paymentSchedules = new ArrayList<>();

    private String deliveryTerms;

    private String legalTerms;

    private String adminNote;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
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

    @DateTimeFormat(pattern = "yyyy-MM-dd")
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

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate buyerIdentityIssuedDate;

    private String buyerAuthorizationInfo;

    private String amountInWords;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
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
}
