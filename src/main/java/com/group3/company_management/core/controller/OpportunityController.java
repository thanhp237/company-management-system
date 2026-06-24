package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
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

@Controller
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    public String listPipeline(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
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
        try {
            String username = authentication.getName();
            Opportunity opportunity = opportunityService.getOpportunityDetail(id, username);
            Map<Long, List<String>> nextStagesByOpportunity =
                    opportunityService.getNextStagesByOpportunity(List.of(opportunity));

            model.addAttribute("opportunity", opportunity);
            model.addAttribute("stages", opportunityService.getStages());
            model.addAttribute("stageCounts", opportunityService.getStageCounts(username));
            model.addAttribute("nextStages", nextStagesByOpportunity.get(opportunity.getId()));
            return "pipeline/detail";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/pipeline";
        }
    }

    @PostMapping("/{id}/stage")
    public String updateStage(
            @PathVariable Long id,
            @RequestParam String stage,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            opportunityService.updateStage(id, stage, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Pipeline stage updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/pipeline/" + id;
    }
}
