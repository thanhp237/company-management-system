package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.service.CustomerActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer-activities")
@RequiredArgsConstructor
public class CustomerActivityController {

    private final CustomerActivityService activityService;

    @GetMapping
    public String listActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type,
            Model model) {

        Page<CustomerActivity> activityPage =
                activityService.getActivities(
                        type,
                        page,
                        10);

        model.addAttribute(
                "activityPage",
                activityPage);

        model.addAttribute(
                "activities",
                activityPage.getContent());

        model.addAttribute(
                "selectedType",
                type);

        return "activity/list";
    }

    @GetMapping("/{id}")
    public String detailActivity(
            @PathVariable Long id,
            Model model) {

        CustomerActivity activity =
                activityService.getActivityById(id);

        model.addAttribute(
                "activity",
                activity);

        return "activity/detail";
    }

    @PostMapping("/{id}/note")
public String updateNote(
        @PathVariable Long id,
        @RequestParam String note,
        @RequestParam Long opportunityId) {

    activityService.updateActivityNote(id, note);

    return "redirect:/pipeline/" + opportunityId;
}
}