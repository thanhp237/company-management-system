package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.SalesTargetSummary;
import com.group3.company_management.core.entity.BusinessSetting;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.SalesTarget;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.BusinessSettingRepository;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.SalesTargetRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.SalesTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesTargetServiceImpl implements SalesTargetService {

    private static final String SALES_EMPLOYEE_TYPE = "Sales Staff";
    private static final String COMMISSION_RATE_KEY = "commissionRate";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final SalesTargetRepository salesTargetRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final BusinessSettingRepository businessSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalesTargetSummary> getTargetSummaries(String username, YearMonth period) {
        User currentUser = getCurrentUser(username);
        assertCanManageTargets(currentUser);

        YearMonth safePeriod = period == null ? YearMonth.now() : period;
        List<Employee> sales = employeeRepository.findByEmployeeType(SALES_EMPLOYEE_TYPE).stream()
                .filter(employee -> employee.getUser() != null && employee.getUser().isActive())
                .sorted(Comparator.comparing(this::saleName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Long, SalesTarget> targetsBySaleId = salesTargetRepository
                .findByTargetYearAndTargetMonthOrderBySaleName(safePeriod.getYear(), safePeriod.getMonthValue())
                .stream()
                .filter(target -> target.getSale() != null)
                .collect(Collectors.toMap(target -> target.getSale().getId(), Function.identity(), (left, right) -> left));

        BigDecimal commissionRate = getCommissionRate();
        LocalDateTime from = safePeriod.atDay(1).atStartOfDay();
        LocalDateTime to = safePeriod.plusMonths(1).atDay(1).atStartOfDay();

        return sales.stream()
                .map(sale -> buildSummary(sale, targetsBySaleId.get(sale.getId()), safePeriod, commissionRate, from, to))
                .toList();
    }

    @Override
    @Transactional
    public void saveTarget(String username,
                           Long saleEmployeeId,
                           Integer targetYear,
                           Integer targetMonth,
                           BigDecimal targetAmount,
                           BigDecimal bonusRate,
                           String note) {
        User currentUser = getCurrentUser(username);
        assertCanManageTargets(currentUser);
        validateTargetInput(saleEmployeeId, targetYear, targetMonth, targetAmount, bonusRate);

        Employee sale = employeeRepository.findById(saleEmployeeId)
                .filter(employee -> SALES_EMPLOYEE_TYPE.equalsIgnoreCase(employee.getEmployeeType()))
                .orElseThrow(() -> new IllegalArgumentException("Nhan vien kinh doanh khong hop le."));

        SalesTarget target = salesTargetRepository
                .findBySaleIdAndTargetYearAndTargetMonth(saleEmployeeId, targetYear, targetMonth)
                .orElseGet(() -> SalesTarget.builder()
                        .sale(sale)
                        .targetYear(targetYear)
                        .targetMonth(targetMonth)
                        .createdBy(currentUser)
                        .build());

        target.setSale(sale);
        target.setTargetAmount(normalizeMoney(targetAmount));
        target.setBonusRate(normalizeRate(bonusRate));
        target.setNote(normalizeNote(note));
        if (target.getCreatedBy() == null) {
            target.setCreatedBy(currentUser);
        }

        salesTargetRepository.save(target);
    }

    private SalesTargetSummary buildSummary(Employee sale,
                                            SalesTarget target,
                                            YearMonth period,
                                            BigDecimal commissionRate,
                                            LocalDateTime from,
                                            LocalDateTime to) {
        BigDecimal targetAmount = target == null ? BigDecimal.ZERO : defaultZero(target.getTargetAmount());
        BigDecimal bonusRate = target == null ? BigDecimal.ZERO : defaultZero(target.getBonusRate());
        BigDecimal achievedAmount = defaultZero(contractRepository.sumFinalAmountBySaleIdAndStatusAndSignedAtBetween(
                sale.getId(), Contract.ContractStatus.SIGNED, from, to));
        BigDecimal remainingAmount = targetAmount.subtract(achievedAmount).max(BigDecimal.ZERO);
        BigDecimal commissionAmount = percentOf(achievedAmount, commissionRate);
        boolean targetReached = targetAmount.compareTo(BigDecimal.ZERO) > 0 && achievedAmount.compareTo(targetAmount) >= 0;
        BigDecimal bonusAmount = targetReached ? percentOf(achievedAmount, bonusRate) : BigDecimal.ZERO;
        BigDecimal achievementRate = targetAmount.compareTo(BigDecimal.ZERO) > 0
                ? achievedAmount.multiply(ONE_HUNDRED).divide(targetAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        User saleUser = sale.getUser();
        return SalesTargetSummary.builder()
                .targetId(target == null ? null : target.getId())
                .saleEmployeeId(sale.getId())
                .saleUserId(saleUser == null ? null : saleUser.getId())
                .saleName(saleName(sale))
                .employeeCode(sale.getEmployeeCode())
                .targetYear(period.getYear())
                .targetMonth(period.getMonthValue())
                .targetAmount(targetAmount)
                .achievedAmount(achievedAmount)
                .remainingAmount(remainingAmount)
                .commissionAmount(commissionAmount)
                .bonusRate(bonusRate)
                .bonusAmount(bonusAmount)
                .achievementRate(achievementRate)
                .targetReached(targetReached)
                .note(target == null ? null : target.getNote())
                .build();
    }

    private void validateTargetInput(Long saleEmployeeId,
                                     Integer targetYear,
                                     Integer targetMonth,
                                     BigDecimal targetAmount,
                                     BigDecimal bonusRate) {
        if (saleEmployeeId == null) {
            throw new IllegalArgumentException("Vui long chon nhan vien kinh doanh.");
        }
        if (targetYear == null || targetYear < 2000 || targetYear > 2100) {
            throw new IllegalArgumentException("Nam target khong hop le.");
        }
        if (targetMonth == null || targetMonth < 1 || targetMonth > 12) {
            throw new IllegalArgumentException("Thang target khong hop le.");
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Doanh so muc tieu khong duoc am.");
        }
        if (bonusRate != null && (bonusRate.compareTo(BigDecimal.ZERO) < 0 || bonusRate.compareTo(ONE_HUNDRED) > 0)) {
            throw new IllegalArgumentException("Ty le thuong phai nam trong khoang 0 - 100%.");
        }
    }

    private void assertCanManageTargets(User user) {
        String roleCode = user.getRole() == null ? "" : user.getRole().getRoleCode();
        if (!List.of("SALES_MANAGER", "MANAGER", "ADMIN").contains(roleCode)) {
            throw new IllegalArgumentException("Ban khong co quyen quan ly target doanh so.");
        }
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsernameAndNotDeleted(username)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan hien tai."));
    }

    private BigDecimal getCommissionRate() {
        return businessSettingRepository.findById(COMMISSION_RATE_KEY)
                .map(BusinessSetting::getSettingValue)
                .map(this::parseRate)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal parseRate(String value) {
        try {
            return normalizeRate(new BigDecimal(value));
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        return defaultZero(amount).multiply(defaultZero(rate)).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private String saleName(Employee employee) {
        return Objects.requireNonNullElse(
                employee.getUser() == null ? null : employee.getUser().getFullName(),
                "Sale #" + employee.getId());
    }
}
