package com.group3.company_management.core.controller;

import com.group3.company_management.core.service.SalesTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/sales-targets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
public class SalesTargetController {

    private final SalesTargetService salesTargetService;

    @GetMapping
    public String listTargets(@RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer month,
                              Authentication authentication,
                              Model model) {
        YearMonth period = resolvePeriod(year, month);
        model.addAttribute("periodYear", period.getYear());
        model.addAttribute("periodMonth", period.getMonthValue());
        try {
            model.addAttribute("targets", salesTargetService.getTargetSummaries(authentication.getName(), period));
        } catch (RuntimeException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("targets", List.of());
        }
        return "sales-targets/list";
    }

    @PostMapping
    public String saveTarget(@RequestParam Long saleEmployeeId,
                             @RequestParam Integer targetYear,
                             @RequestParam Integer targetMonth,
                             @RequestParam BigDecimal targetAmount,
                             @RequestParam(defaultValue = "0") BigDecimal bonusRate,
                             @RequestParam(required = false) String note,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            salesTargetService.saveTarget(authentication.getName(), saleEmployeeId, targetYear, targetMonth, targetAmount, bonusRate, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu target doanh số.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/sales-targets?year=" + targetYear + "&month=" + targetMonth;
    }

    private YearMonth resolvePeriod(Integer year, Integer month) {
        if (year == null || month == null) {
            return YearMonth.now();
        }
        return YearMonth.of(year, month);
    }
}
