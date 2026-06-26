package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findByActiveTrue();
    List<Voucher> findByActiveTrueAndExpiredAtAfter(LocalDateTime now);
}