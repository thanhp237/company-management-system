
package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.CustomerActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerActivityRepository
        extends JpaRepository<CustomerActivity, Long> {

    Page<CustomerActivity> findByActivityTypeIgnoreCase(
            String activityType,
            Pageable pageable);

    List<CustomerActivity> findByCustomerIdOrderByCreatedAtDesc(
            Long customerId);

    Page<CustomerActivity> findByCustomerId(
            Long customerId,
            Pageable pageable);

    long countByCustomerId(Long customerId);
}