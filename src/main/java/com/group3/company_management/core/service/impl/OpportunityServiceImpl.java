package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OpportunityServiceImpl implements OpportunityService {

    private static final List<String> STAGES = List.of(
            "NEW",
            "QUALIFIED",
            "PROPOSAL",
            "NEGOTIATION",
            "WON",
            "LOST"
    );

    private static final Map<String, List<String>> NEXT_STAGES = Map.of(
            "NEW", List.of("QUALIFIED", "LOST"),
            "QUALIFIED", List.of("PROPOSAL", "LOST"),
            "PROPOSAL", List.of("NEGOTIATION", "LOST"),
            "NEGOTIATION", List.of("WON", "LOST"),
            "WON", List.of(),
            "LOST", List.of()
    );

    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    @Override
    public List<String> getStages() {
        return STAGES;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Opportunity> getPipeline(String stage) {
        String normalizedStage = normalizeStageFilter(stage);
        if (normalizedStage == null) {
            return opportunityRepository.findAllByOrderByCreatedAtDesc();
        }

        return opportunityRepository.findByStageOrderByCreatedAtDesc(normalizedStage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Opportunity> getPipelinePage(String keyword, String stage, int page, int size, String username) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStage = normalizeStageFilter(stage);
        User currentUser = findCurrentUser(username);
        String assignedUsername = canViewAll(currentUser) ? null : username;

        return opportunityRepository.searchPipeline(
                normalizedKeyword,
                normalizedStage,
                assignedUsername,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Opportunity getOpportunityDetail(Long id, String username) {
        Opportunity opportunity = opportunityRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found"));
        validateAccess(opportunity, findCurrentUser(username));
        return opportunity;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStageCounts(String username) {
        User currentUser = findCurrentUser(username);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String stage : STAGES) {
            long count = canViewAll(currentUser)
                    ? opportunityRepository.countByStage(stage)
                    : opportunityRepository.countByStageAndAssignedToUsername(stage, username);
            counts.put(stage, count);
        }
        return counts;
    }

    @Override
    public Map<Long, List<String>> getNextStagesByOpportunity(List<Opportunity> opportunities) {
        Map<Long, List<String>> nextStages = new LinkedHashMap<>();
        for (Opportunity opportunity : opportunities) {
            nextStages.put(opportunity.getId(), getNextStages(opportunity.getStage()));
        }
        return nextStages;
    }

    @Override
    public void updateStage(Long id, String stage, String username) {
        Opportunity opportunity = opportunityRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found"));
        User currentUser = findCurrentUser(username);
        validateAccess(opportunity, currentUser);

        String nextStage = normalizeRequiredStage(stage);
        String currentStage = normalizeRequiredStage(opportunity.getStage());

        if (currentStage.equals(nextStage)) {
            return;
        }

        if (!getNextStages(currentStage).contains(nextStage)) {
            throw new IllegalArgumentException("Invalid stage transition: " + currentStage + " -> " + nextStage);
        }

        opportunity.setStage(nextStage);
        opportunityRepository.save(opportunity);
    }

    private List<String> getNextStages(String stage) {
        return new ArrayList<>(NEXT_STAGES.getOrDefault(normalizeRequiredStage(stage), List.of()));
    }

    private String normalizeStageFilter(String stage) {
        if (stage == null || stage.isBlank()) {
            return null;
        }

        String normalized = normalizeRequiredStage(stage);
        return "ALL".equals(normalized) ? null : normalized;
    }

    private String normalizeRequiredStage(String stage) {
        if (stage == null || stage.isBlank()) {
            throw new IllegalArgumentException("Stage is required");
        }

        String normalized = stage.trim().toUpperCase(Locale.ROOT);
        if (!STAGES.contains(normalized) && !"ALL".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported stage: " + stage);
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private User findCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Current user is required");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private boolean canViewAll(User user) {
        if (user.getRole() == null || user.getRole().getRoleCode() == null) {
            return false;
        }
        String roleCode = user.getRole().getRoleCode().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(roleCode) || "MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode);
    }

    private void validateAccess(Opportunity opportunity, User user) {
        if (canViewAll(user)) {
            return;
        }
        if (opportunity.getAssignedTo() == null ||
                !user.getUsername().equals(opportunity.getAssignedTo().getUsername())) {
            throw new IllegalArgumentException("You are not allowed to access this opportunity");
        }
    }
}
