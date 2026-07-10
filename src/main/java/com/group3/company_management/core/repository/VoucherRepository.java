package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findByActiveTrue();

    List<Voucher> findByActiveTrueAndExpiredAtAfter(LocalDateTime now);

    Optional<Voucher> findByVoucherCodeIgnoreCase(String voucherCode);

    boolean existsByVoucherCodeIgnoreCase(String voucherCode);

    @Query("""
            select v from Voucher v
            where v.active = true
              and (v.expiredAt is null or v.expiredAt > :now)
            order by v.expiredAt asc, v.voucherCode asc
            """)
    List<Voucher> findUsableVouchers(@Param("now") LocalDateTime now);
}
