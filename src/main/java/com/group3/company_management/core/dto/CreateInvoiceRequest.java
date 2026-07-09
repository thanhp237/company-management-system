package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateInvoiceRequest {
    private Long paymentScheduleId;
    private LocalDate dueDate;
    private String note;
    private String status;
    private List<InvoiceItemFormRow> items = new ArrayList<>();
}
