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

import com.group3.company_management.core.service.NotificationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.group3.company_management.core.dto.OpportunityEvaluationResult;
import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.repository.CustomerActivityRepository;

import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
@Transactional
public class OpportunityServiceImpl implements OpportunityService {
    private final CustomerActivityRepository customerActivityRepository;
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
    // 
    private final NotificationService notificationService;

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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cơ hội bán hàng"));
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cơ hội bán hàng"));
        User currentUser = findCurrentUser(username);
        validateAccess(opportunity, currentUser);

        String nextStage = normalizeRequiredStage(stage);
        String currentStage = normalizeRequiredStage(opportunity.getStage());

        if (currentStage.equals(nextStage)) {
            return;
        }

        if (!getNextStages(currentStage).contains(nextStage)) {
            throw new IllegalArgumentException("Không thể chuyển giai đoạn từ " + currentStage + " sang " + nextStage);
        }

        opportunity.setStage(nextStage);
        opportunityRepository.save(opportunity);

        // THAY ĐỔI TẠI ĐÂY: Truyền currentUser.getId() thay vì opportunity.getAssignedTo().getId()
        if (currentUser != null) {
            notificationService.createNotification(
                    currentUser.getId(), // Bắn chuẩn xác về ID 19 của người vừa bấm nút
                    "🔄 Cập nhật trạng thái Cơ hội",
                        "Bạn đã chuyển trạng thái cơ hội của khách '" + opportunity.getCustomer().getFullName() + "' từ " + displayStage(currentStage) + " sang " + displayStage(nextStage)
            );
        }
    }
    @Override
    @Transactional(readOnly = true)
    public OpportunityEvaluationResult evaluateOpportunity(Long id, String username) {
        Opportunity opportunity = getOpportunityDetail(id, username);

        List<CustomerActivity> activities =
                customerActivityRepository.findByRelatedTypeIgnoreCaseAndRelatedIdOrderByCreatedAtDesc(
                        "OPPORTUNITY",
                        opportunity.getId()
                );

        if (activities.isEmpty() && opportunity.getCustomer() != null) {
            activities = customerActivityRepository.findByCustomerIdOrderByCreatedAtDesc(
                    opportunity.getCustomer().getId()
            );
        }

        int budgetScore = calculateBudgetScore(opportunity);
        int decisionMakerScore = calculateDecisionMakerScore(activities);
        int needScore = calculateNeedScore(activities);
        int engagementScore = calculateEngagementScore(activities);
        int companySizeScore = calculateCompanySizeScore(activities);

        int totalScore = budgetScore
                + decisionMakerScore
                + needScore
                + engagementScore
                + companySizeScore;

        String classification;
        String suggestedStage;

        if (totalScore >= 70) {
            classification = "QUALIFIED";
            suggestedStage = "QUALIFIED";
        } else if (totalScore >= 40) {
            classification = "NEED_NURTURE";
            suggestedStage = "NEW";
        } else {
            classification = "DISQUALIFIED";
            suggestedStage = "LOST";
        }

        return OpportunityEvaluationResult.builder()
                .budgetScore(budgetScore)
                .decisionMakerScore(decisionMakerScore)
                .needScore(needScore)
                .engagementScore(engagementScore)
                .companySizeScore(companySizeScore)
                .totalScore(totalScore)
                .classification(classification)
                .suggestedStage(suggestedStage)
                .reasons(List.of(
                        "Ngân sách: " + budgetScore + "/30",
                        "Người quyết định: " + decisionMakerScore + "/20",
                        "Nhu cầu: " + needScore + "/20",
                        "Mức độ tương tác: " + engagementScore + "/15",
                        "Quy mô công ty: " + companySizeScore + "/15"
                ))
                .build();
    }

    @Override
    @Transactional
    public void confirmEvaluation(Long id, String username) {
        Opportunity opportunity = opportunityRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cơ hội bán hàng"));

        User currentUser = findCurrentUser(username);
        validateAssignedSales(opportunity, currentUser);

        OpportunityEvaluationResult result = evaluateOpportunity(id, username);

        String oldStage = opportunity.getStage();
        opportunity.setStage(result.getSuggestedStage());
        opportunityRepository.save(opportunity);

        CustomerActivity audit = CustomerActivity.builder()
                .customerId(opportunity.getCustomer().getId())
                .activityType("NOTE")
                .activityNote("Kết quả đánh giá: điểm " + result.getTotalScore()
                        + "/100, phân loại " + displayClassification(result.getClassification())
                        + ". Giai đoạn: " + displayStage(oldStage) + " -> " + displayStage(result.getSuggestedStage()))
                .relatedType("OPPORTUNITY")
                .relatedId(opportunity.getId())
                .employeeId(currentUser.getId())
                .build();

        customerActivityRepository.save(audit);

        if (!oldStage.equals(result.getSuggestedStage()) && opportunity.getAssignedTo() != null) {
            notificationService.createNotification(
                    opportunity.getAssignedTo().getId(),
                    " Đánh giá Cơ hội tự động",
                    "Hệ thống vừa chấm điểm cơ hội của khách '" + opportunity.getCustomer().getFullName() + "'. Giai đoạn thay đổi: " + displayStage(oldStage) + " -> " + displayStage(result.getSuggestedStage())
            );
        }
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
            throw new IllegalArgumentException("Vui lòng chọn giai đoạn");
        }

        String normalized = stage.trim().toUpperCase(Locale.ROOT);
        if (!STAGES.contains(normalized) && !"ALL".equals(normalized)) {
            throw new IllegalArgumentException("Giai đoạn không hợp lệ: " + stage);
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
            throw new IllegalArgumentException("Vui lòng đăng nhập để tiếp tục");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đang đăng nhập"));
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
            throw new IllegalArgumentException("Bạn không có quyền truy cập cơ hội này");
        }
    }
    private int calculateBudgetScore(Opportunity opportunity) {
        if (opportunity.getExpectedAmount() == null) {
            return 0;
        }

        if (opportunity.getExpectedAmount().compareTo(new BigDecimal("100000000")) >= 0) {
            return 30;
        }

        if (opportunity.getExpectedAmount().compareTo(BigDecimal.ZERO) > 0) {
            return 15;
        }

        return 0;
    }

    private int calculateDecisionMakerScore(List<CustomerActivity> activities) {
        String text = joinActivityText(activities);

        return containsAny(text,
                "ceo",
                "director",
                "manager",
                "giám đốc",
                "truong phong",
                "trưởng phòng",
                "quan ly",
                "quản lý") ? 20 : 0;
    }

    private int calculateNeedScore(List<CustomerActivity> activities) {
        String text = joinActivityText(activities);

        return containsAny(text,
                "need",
                "problem",
                "requirement",
                "urgent",
                "nhu cầu",
                "van de",
                "vấn đề",
                "can",
                "cần",
                "gap",
                "gấp",
                "yeu cau",
                "yêu cầu") ? 20 : 0;
    }

    private int calculateEngagementScore(List<CustomerActivity> activities) {
        return activities.stream()
                .anyMatch(activity -> containsAny(
                        safeLower(activity.getActivityType()),
                        "call",
                        "email",
                        "meeting"
                )) ? 15 : 0;
    }

    private int calculateCompanySizeScore(List<CustomerActivity> activities) {
        String text = joinActivityText(activities);

        return containsAny(text,
                "10 employees",
                "more than 10",
                "over 10",
                "10 nhân viên",
                "tren 10",
                "trên 10",
                ">10",
                ">= 10") ? 15 : 0;
    }

    private String joinActivityText(List<CustomerActivity> activities) {
        return activities.stream()
                .map(activity -> safeLower(activity.getActivityType()) + " " + safeLower(activity.getActivityNote()))
                .reduce("", (current, next) -> current + " " + next);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String displayStage(String stage) {
        if (stage == null) {
            return "chưa xác định";
        }
        return switch (stage.trim().toUpperCase(Locale.ROOT)) {
            case "NEW" -> "Mới";
            case "QUALIFIED" -> "Đã đủ điều kiện";
            case "PROPOSAL" -> "Đề xuất";
            case "NEGOTIATION" -> "Đàm phán";
            case "WON" -> "Thắng";
            case "LOST" -> "Thua";
            default -> stage;
        };
    }

    private String displayClassification(String classification) {
        if (classification == null) {
            return "chưa xác định";
        }
        return switch (classification.trim().toUpperCase(Locale.ROOT)) {
            case "QUALIFIED" -> "đủ điều kiện";
            case "NEED_NURTURE" -> "cần chăm sóc thêm";
            case "DISQUALIFIED" -> "không đủ điều kiện";
            default -> classification;
        };
    }

    private void validateAssignedSales(Opportunity opportunity, User user) {
        if (user.getRole() == null
                || user.getRole().getRoleCode() == null
                || !"SALES".equalsIgnoreCase(user.getRole().getRoleCode())) {
            throw new IllegalArgumentException("Chỉ nhân viên kinh doanh được phân công mới có thể đánh giá cơ hội này");
        }

        if (opportunity.getAssignedTo() == null
                || !user.getUsername().equals(opportunity.getAssignedTo().getUsername())) {
            throw new IllegalArgumentException("Bạn chưa được phân công cho cơ hội này");
        }
    }
}
