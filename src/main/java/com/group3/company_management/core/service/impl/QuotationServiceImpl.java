package com.group3.company_management.core.service.impl;



import com.group3.company_management.core.dto.QuotationDetailRequest;
import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationDetailResponse;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.entity.Quotation;
import com.group3.company_management.core.entity.QuotationDetail;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.service.QuotationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long createQuotation(QuotationRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setVoucherId(request.getVoucherId());
        quotation.setOpportunityId(request.getOpportunityId());
        quotation.setNote(request.getNote());
        quotation.setStatus("DRAFT");
        quotation.setDiscountAmount(BigDecimal.ZERO);

        BigDecimal subTotal = BigDecimal.ZERO;

        for (QuotationDetailRequest item : request.getDetails()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            BigDecimal totalPrice = unitPrice.multiply(quantity);

            QuotationDetail detail = new QuotationDetail();
            detail.setQuotation(quotation);
            detail.setProduct(product);
            detail.setServiceName(product.getName());
            detail.setDescription(product.getDescription());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(unitPrice);
            detail.setTotalPrice(totalPrice);

            quotation.getDetails().add(detail);

            subTotal = subTotal.add(totalPrice);
        }

        quotation.setSubTotal(subTotal);
        quotation.setFinalAmount(subTotal.subtract(quotation.getDiscountAmount()));

        quotation.setQuotationCode(
                "QT-" + System.currentTimeMillis()
        );

        Quotation saved = quotationRepository.save(quotation);

        return saved.getId();
    }

    @Override
    public QuotationResponse getQuotationDetail(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        return mapToResponse(quotation);
    }

    @Override
    public QuotationResponse previewQuotation(QuotationRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        QuotationResponse response = new QuotationResponse();
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setCustomerEmail(customer.getEmail());
        response.setCustomerPhone(customer.getPhone());
        response.setCustomerAddress(customer.getAddress());
        response.setNote(request.getNote());
        response.setStatus("PREVIEW");

        BigDecimal subTotal = BigDecimal.ZERO;
        List<QuotationDetailResponse> detailResponses = new ArrayList<>();

        for (QuotationDetailRequest item : request.getDetails()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            QuotationDetailResponse detailResponse = new QuotationDetailResponse();
            detailResponse.setProductId(product.getId());
            detailResponse.setProductCode(product.getProductCode());
            detailResponse.setProductName(product.getName());
            detailResponse.setDescription(product.getDescription());
            detailResponse.setQuantity(item.getQuantity());
            detailResponse.setUnitPrice(unitPrice);
            detailResponse.setTotalPrice(totalPrice);

            detailResponses.add(detailResponse);
            subTotal = subTotal.add(totalPrice);
        }

        response.setDetails(detailResponses);
        response.setSubTotal(subTotal);
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setFinalAmount(subTotal);

        return response;
    }

    private QuotationResponse mapToResponse(Quotation quotation) {
        QuotationResponse response = new QuotationResponse();

        response.setId(quotation.getId());
        response.setQuotationCode(quotation.getQuotationCode());

        Customer customer = quotation.getCustomer();
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setCustomerEmail(customer.getEmail());
        response.setCustomerPhone(customer.getPhone());
        response.setCustomerAddress(customer.getAddress());

        response.setSubTotal(quotation.getSubTotal());
        response.setDiscountAmount(quotation.getDiscountAmount());
        response.setFinalAmount(quotation.getFinalAmount());
        response.setStatus(quotation.getStatus());
        response.setNote(quotation.getNote());
        response.setCreatedAt(quotation.getCreatedAt());

        List<QuotationDetailResponse> details = quotation.getDetails()
                .stream()
                .map(detail -> {
                    Product product = detail.getProduct();

                    QuotationDetailResponse d = new QuotationDetailResponse();
                    d.setProductId(product.getId());
                    d.setProductCode(product.getProductCode());
                    d.setProductName(product.getName());
                    d.setDescription(detail.getDescription());
                    d.setQuantity(detail.getQuantity());
                    d.setUnitPrice(detail.getUnitPrice());
                    d.setTotalPrice(detail.getTotalPrice());

                    return d;
                })
                .toList();

        response.setDetails(details);

        return response;
    }
}