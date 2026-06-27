package com.group3.company_management.core.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractStatisticsResponse {
    private long total;
    private long draft;
    private long pending;
    private long reviewed;
    private long sent;
    private long signed;
    private long cancelled;
}