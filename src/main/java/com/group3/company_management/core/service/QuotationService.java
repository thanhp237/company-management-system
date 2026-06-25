package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;

public interface QuotationService {

    Long createQuotation(QuotationRequest request);

    QuotationResponse getQuotationDetail(Long id);

    QuotationResponse previewQuotation(QuotationRequest request);
}