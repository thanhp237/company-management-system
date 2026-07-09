package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.util.Map;

@Controller @RequiredArgsConstructor
public class VnPayController {
    private final VnPayService vnPayService;
    @PostMapping("/customer/portal/payments/{invoiceId}/pay")
    public RedirectView pay(@PathVariable Long invoiceId, HttpServletRequest request, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Customer customer))
            throw new RuntimeException("Khách hàng chưa đăng nhập.");
        String ip=request.getHeader("X-Forwarded-For");
        if(ip==null||ip.isBlank()) ip=request.getRemoteAddr(); else ip=ip.split(",")[0].trim();
        return new RedirectView(vnPayService.createUrl(invoiceId,customer.getId(),ip));
    }
    @RequestMapping(value = "/payments/vnpay/ipn", method = {RequestMethod.GET, RequestMethod.POST}) @ResponseBody
    public ResponseEntity<Map<String,String>> ipn(@RequestParam Map<String,String> params) {
        return ResponseEntity.ok(vnPayService.ipn(params));
    }
    @GetMapping("/payments/vnpay/return")
    public String result(@RequestParam Map<String,String> params) {
        if(!vnPayService.valid(params)) return "redirect:/customer/portal/payments?payment=invalid";
        if("00".equals(params.get("vnp_ResponseCode"))) {
            return "redirect:/customer/portal/payments?payment="+
                    (vnPayService.confirmFromReturn(params) ? "success" : "processing");
        }
        return "redirect:/customer/portal/payments?payment="+
                ("00".equals(params.get("vnp_ResponseCode"))?"processing":"failed");
    }
}
