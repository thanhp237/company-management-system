package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.SalesTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {
    Optional<SalesTarget> findBySaleIdAndTargetYearAndTargetMonth(Long saleId, Integer targetYear, Integer targetMonth);

    @Query("""
            select t
            from SalesTarget t
            left join t.sale sale
            left join sale.user user
            where t.targetYear = :targetYear
            and t.targetMonth = :targetMonth
            order by user.fullName asc
            """)
    List<SalesTarget> findByTargetYearAndTargetMonthOrderBySaleName(
            @Param("targetYear") Integer targetYear,
            @Param("targetMonth") Integer targetMonth);
}
