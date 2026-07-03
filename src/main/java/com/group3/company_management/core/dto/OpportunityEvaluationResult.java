package com.group3.company_management.core.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OpportunityEvaluationResult {
    private int budgetScore;
    private int decisionMakerScore;
    private int needScore;
    private int engagementScore;
    private int companySizeScore;
    private int totalScore;
    private String classification;
    private String suggestedStage;
    private List<String> reasons;
}