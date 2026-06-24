package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Opportunity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface OpportunityService {

    List<String> getStages();

    List<Opportunity> getPipeline(String stage);

    Page<Opportunity> getPipelinePage(String keyword, String stage, int page, int size, String username);

    Opportunity getOpportunityDetail(Long id, String username);

    Map<String, Long> getStageCounts(String username);

    Map<Long, List<String>> getNextStagesByOpportunity(List<Opportunity> opportunities);

    void updateStage(Long id, String stage, String username);
}
