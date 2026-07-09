package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    List<PaymentSchedule> findByContractIdOrderByInstallmentNo(Long contractId);
    void deleteByContractId(Long contractId);
}
