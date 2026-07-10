package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Voucher;
import com.group3.company_management.core.entity.BusinessSetting;
import com.group3.company_management.core.repository.BusinessSettingRepository;
import com.group3.company_management.core.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

@Controller
@RequestMapping("/business-rules")
@PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','ADMINOFFICER')")
@RequiredArgsConstructor
public class BusinessRuleController {

    private static final String COMMISSION_RATE_KEY = "commissionRate";
    private static final int DEFAULT_COMMISSION_RATE = 5;

    private final BusinessSettingRepository businessSettingRepository;
    private final VoucherRepository voucherRepository;

    @GetMapping
    public String showRules(Model model) {
        model.addAttribute("commissionRate", getCommissionRate());
        model.addAttribute("vouchers", voucherRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Voucher::getId).reversed())
                .toList());
        return "admin/business-rules";
    }

    @PostMapping("/save")
    public String saveRules(
            @RequestParam(required = false) Integer commissionRate,
            RedirectAttributes redirectAttributes) {

        if (commissionRate == null || commissionRate < 0 || commissionRate > 100) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tỷ lệ hoa hồng phải nằm trong khoảng 0-100%.");
            return "redirect:/business-rules";
        }

        BusinessSetting setting = businessSettingRepository.findById(COMMISSION_RATE_KEY)
                .orElseGet(() -> {
                    BusinessSetting newSetting = new BusinessSetting();
                    newSetting.setSettingKey(COMMISSION_RATE_KEY);
                    return newSetting;
                });
        setting.setSettingValue(String.valueOf(commissionRate));
        businessSettingRepository.save(setting);

        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu tỷ lệ hoa hồng " + commissionRate + "%.");
        return "redirect:/business-rules";
    }

    @PostMapping("/vouchers")
    public String saveVoucher(
            @RequestParam(required = false) Long id,
            @RequestParam String voucherCode,
            @RequestParam BigDecimal discountPercent,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime expiredAt,
            @RequestParam(required = false, defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes) {

        String normalizedCode = normalizeVoucherCode(voucherCode);
        if (normalizedCode.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher không được bỏ trống.");
            return "redirect:/business-rules";
        }
        if (discountPercent == null
                || discountPercent.compareTo(BigDecimal.ZERO) <= 0
                || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phần trăm giảm giá phải lớn hơn 0 và không vượt quá 100%.");
            return "redirect:/business-rules";
        }
        if (id == null && voucherRepository.existsByVoucherCodeIgnoreCase(normalizedCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại.");
            return "redirect:/business-rules";
        }

        Voucher voucher = id == null
                ? new Voucher()
                : voucherRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));
        if (id != null
                && !voucher.getVoucherCode().equalsIgnoreCase(normalizedCode)
                && voucherRepository.existsByVoucherCodeIgnoreCase(normalizedCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã voucher đã tồn tại.");
            return "redirect:/business-rules";
        }

        voucher.setVoucherCode(normalizedCode);
        voucher.setDiscountPercent(discountPercent);
        voucher.setMaxDiscountAmount(maxDiscountAmount == null ? BigDecimal.ZERO : maxDiscountAmount);
        voucher.setExpiredAt(expiredAt);
        voucher.setActive(active);
        voucherRepository.save(voucher);

        redirectAttributes.addFlashAttribute("successMessage", id == null
                ? "Đã tạo voucher mới."
                : "Đã cập nhật voucher.");
        return "redirect:/business-rules";
    }

    @PostMapping("/vouchers/{id}/toggle")
    public String toggleVoucher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));
        voucher.setActive(!Boolean.TRUE.equals(voucher.getActive()));
        voucherRepository.save(voucher);
        redirectAttributes.addFlashAttribute("successMessage", Boolean.TRUE.equals(voucher.getActive())
                ? "Đã bật voucher."
                : "Đã tắt voucher.");
        return "redirect:/business-rules";
    }

    private String normalizeVoucherCode(String voucherCode) {
        if (voucherCode == null) {
            return "";
        }
        return voucherCode.trim().toUpperCase();
    }

    private int getCommissionRate() {
        return businessSettingRepository.findById(COMMISSION_RATE_KEY)
                .map(BusinessSetting::getSettingValue)
                .map(this::parseCommissionRate)
                .orElse(DEFAULT_COMMISSION_RATE);
    }

    private int parseCommissionRate(String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0 || value > 100) {
                return DEFAULT_COMMISSION_RATE;
            }
            return value;
        } catch (NumberFormatException exception) {
            return DEFAULT_COMMISSION_RATE;
        }
    }
}
