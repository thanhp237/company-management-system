package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Voucher;
import com.group3.company_management.core.repository.BusinessSettingRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.group3.company_management.core.security.JwtTokenProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BusinessRuleController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class BusinessRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusinessSettingRepository businessSettingRepository;

    @MockBean
    private VoucherRepository voucherRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean(name = "customUserDetailsService")
    private UserDetailsService userDetailsService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void businessRulesPageRendersVoucherBelowCommission() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setId(1L);
        voucher.setVoucherCode("VIP10");
        voucher.setDiscountPercent(BigDecimal.TEN);
        voucher.setMaxDiscountAmount(BigDecimal.valueOf(2_000_000));
        voucher.setExpiredAt(LocalDateTime.of(2026, 7, 22, 15, 50));
        voucher.setActive(true);

        when(businessSettingRepository.findById("commissionRate")).thenReturn(Optional.empty());
        when(voucherRepository.findAll()).thenReturn(List.of(voucher));

        mockMvc.perform(get("/business-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tỷ lệ hoa hồng")))
                .andExpect(content().string(containsString("Cấu hình Voucher")))
                .andExpect(content().string(containsString("business-rules-layout")));
    }
}
