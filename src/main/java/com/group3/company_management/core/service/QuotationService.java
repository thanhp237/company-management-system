package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;

public interface QuotationService {

    Long createQuotation(QuotationRequest request, String username);

    QuotationResponse getQuotationDetail(Long id);

    void acceptQuotation(Long id, String username);

    QuotationResponse previewQuotation(QuotationRequest request);
}
