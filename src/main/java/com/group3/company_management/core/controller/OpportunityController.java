package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.entity.Quotation;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.service.OpportunityService;
import com.group3.company_management.core.controller.CustomerActivityController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class OpportunityController {

    private static final Set<String> QUOTATION_STAGES = Set.of("QUALIFIED", "PROPOSAL", "NEGOTIATION");
    private static final Set<String> CONTRACT_QUOTATION_STATUSES = Set.of("APPROVED", "ACCEPTED");

    private final OpportunityService opportunityService;
    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;

    @GetMapping
    public String listPipeline(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        if (!canAccessPipeline(authentication)) {
            return "redirect:/contracts";
        }
        String username = authentication.getName();
        Page<Opportunity> opportunityPage = opportunityService.getPipelinePage(null, null, page, 10, username);
        List<Opportunity> opportunities = opportunityPage.getContent();

        model.addAttribute("opportunityPage", opportunityPage);
        model.addAttribute("opportunities", opportunities);
        model.addAttribute("countAccount", opportunityPage.getTotalElements());
        model.addAttribute("stages", opportunityService.getStages());
        model.addAttribute("keyword", "");
        model.addAttribute("selectedStage", "ALL");
        model.addAttribute("isSearch", false);
        model.addAttribute("stageCounts", opportunityService.getStageCounts(username));
        return "pipeline/list";
    }

    @GetMapping("/find")
    public String findPipeline(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        if (!canAccessPipeline(authentication)) {
            return "redirect:/contracts";
        }
        String username = authentication.getName();
        Page<Opportunity> opportunityPage = opportunityService.getPipelinePage(keyword, stage, page, 10, username);
        List<Opportunity> opportunities = opportunityPage.getContent();
        String selectedStage = stage == null || stage.isBlank() ? "ALL" : stage.toUpperCase();

        model.addAttribute("opportunityPage", opportunityPage);
        model.addAttribute("opportunities", opportunities);
        model.addAttribute("countAccount", opportunityPage.getTotalElements());
        model.addAttribute("stages", opportunityService.getStages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStage", selectedStage);
        model.addAttribute("isSearch", true);
        model.addAttribute("stageCounts", opportunityService.getStageCounts(username));
        return "pipeline/list";
    }

    @GetMapping("/{id}")
    public String detailPipeline(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!canAccessPipeline(authentication)) {
            return "redirect:/contracts";
        }
        try {
            String username = authentication.getName();
            Opportunity opportunity = opportunityService.getOpportunityDetail(id, username);
            Map<Long, List<String>> nextStagesByOpportunity =
                    opportunityService.getNextStagesByOpportunity(List.of(opportunity));
            Quotation existingQuotation =
                    quotationRepository.findFirstByOpportunityIdOrderByCreatedAtDesc(opportunity.getId()).orElse(null);
            Contract existingContract = existingQuotation == null
                    ? null
                    : contractRepository.findByQuotationId(existingQuotation.getId()).orElse(null);

            model.addAttribute("opportunity", opportunity);
            model.addAttribute("stages", opportunityService.getStages());
            model.addAttribute("stageCounts", opportunityService.getStageCounts(username));
            model.addAttribute("nextStages", nextStagesByOpportunity.get(opportunity.getId()));
            model.addAttribute("existingQuotation", existingQuotation);
            model.addAttribute("existingContract", existingContract);
            model.addAttribute("canCreateQuotation", canCreateQuotation(authentication, opportunity, existingQuotation));
            model.addAttribute("canCreateContract", canCreateContract(authentication, opportunity, existingQuotation, existingContract));
            model.addAttribute("quotationReadyForContract", quotationReadyForContract(existingQuotation));
            model.addAttribute("canAddActivity", !isClosedStage(opportunity.getStage()));
            return "pipeline/detail";

            

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/pipeline";
        }
    }

    @GetMapping("/{id}/evaluation")
    public String evaluateOpportunity(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!canAccessPipeline(authentication)) {
            return "redirect:/contracts";
        }
        try {
            String username = authentication.getName();
            Opportunity opportunity = opportunityService.getOpportunityDetail(id, username);
            var evaluation = opportunityService.evaluateOpportunity(id, username);
            int score = evaluation.getTotalScore();

            model.addAttribute("opportunity", opportunity);
            model.addAttribute("evaluation", evaluation);
            model.addAttribute("score", score);
            model.addAttribute("classification", evaluation.getClassification());
            model.addAttribute("confidence", score >= 80 ? "94%" : score >= 60 ? "78%" : "52%");
            model.addAttribute("rank", score >= 80 ? "A+" : score >= 60 ? "B" : "C");
            model.addAttribute("stageCounts", opportunityService.getStageCounts(username));

            return "pipeline/evaluation";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/pipeline";
        }
    }
    @PostMapping("/{id}/evaluation")
    public String confirmEvaluation(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (!canAccessPipeline(authentication)) {
            return "redirect:/contracts";
        }
        try {
            opportunityService.confirmEvaluation(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Opportunity evaluated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/pipeline/" + id;
    }

// @PostMapping("/{id}/activity")
//             public String updateActivityNote(
//                 @PathVariable Long id,
//                 @RequestParam String note) {
        
//                 activityService.updateActivityNote(id, note);
        
//                 return "redirect:/customer-activities/" + id;
//         }
    
    

    @PostMapping("/{id}/stage")
    public String updateStage(
            @PathVariable Long id,
            @RequestParam String stage,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (!canAccessPipeline(authentication)) {
            return "redirect:/contracts";
        }
        try {
            opportunityService.updateStage(id, stage, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Pipeline stage updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/pipeline/" + id;
    }

    private boolean canCreateQuotation(Authentication authentication, Opportunity opportunity, Object existingQuotation) {
        if (existingQuotation != null
                || opportunity == null
                || opportunity.getCustomer() == null
                || opportunity.getStage() == null) {
            return false;
        }

        return QUOTATION_STAGES.contains(opportunity.getStage().toUpperCase())
                && hasAnyRole(authentication, "ROLE_SALES", "ROLE_MANAGER", "ROLE_SALES_MANAGER", "ROLE_ADMIN");
    }

    private boolean canAccessPipeline(Authentication authentication) {
        return hasAnyRole(authentication, "ROLE_SALES", "ROLE_MANAGER", "ROLE_SALES_MANAGER", "ROLE_ADMIN");
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null) {
            return false;
        }
        Set<String> allowedRoles = Set.of(roles);
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowedRoles::contains);
    }

    private boolean isClosedStage(String stage) {
        if (stage == null) {
            return false;
        }
        String normalized = stage.trim().toUpperCase();
        return "WON".equals(normalized) || "LOST".equals(normalized);
    }

    private boolean canCreateContract(
            Authentication authentication,
            Opportunity opportunity,
            Quotation quotation,
            Contract existingContract) {
        if (opportunity == null || quotation == null || existingContract != null) {
            return false;
        }
        return "WON".equalsIgnoreCase(opportunity.getStage())
                && quotationReadyForContract(quotation)
                && hasAnyRole(authentication, "ROLE_SALES", "ROLE_MANAGER", "ROLE_SALES_MANAGER", "ROLE_ADMIN");
    }

    private boolean quotationReadyForContract(Quotation quotation) {
        if (quotation == null || quotation.getStatus() == null) {
            return false;
        }
        return CONTRACT_QUOTATION_STATUSES.contains(quotation.getStatus().trim().toUpperCase());
    }


}
