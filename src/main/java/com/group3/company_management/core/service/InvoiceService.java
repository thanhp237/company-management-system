package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.CreateInvoiceRequest;
import com.group3.company_management.core.entity.Invoice;
import java.util.List;

public interface InvoiceService {

    Invoice createInvoice(Long contractId, CreateInvoiceRequest request);

    Invoice issueInvoice(Long invoiceId);

    Invoice getInvoiceById(Long invoiceId);

    List<Invoice> getAllInvoices();

    int syncDraftInvoicesForSignedContracts();

    List<Invoice> getAllInvoicesFiltered(String search, String status, String sortBy, String order);

    CreateInvoiceRequest prepareCreateRequest(Long contractId);

    CreateInvoiceRequest prepareEditRequest(Long invoiceId);

    Invoice updateInvoice(Long id, CreateInvoiceRequest request);

    String generateNextInvoiceCode();

    void deleteInvoice(Long id);
}
