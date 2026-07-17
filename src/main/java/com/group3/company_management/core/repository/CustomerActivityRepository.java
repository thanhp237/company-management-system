
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

    Page<CustomerActivity> findByCustomerIdAndActivityTypeIgnoreCase(
            Long customerId,
            String activityType,
            Pageable pageable);

    long countByCustomerId(Long customerId);
    List<CustomerActivity> findByRelatedTypeIgnoreCaseAndRelatedIdOrderByCreatedAtDesc(
            String relatedType,
            Long relatedId
    );
    Page<CustomerActivity> findByEmployeeIdIn(List<Long> employeeIds, Pageable pageable);
    Page<CustomerActivity> findByEmployeeIdInAndActivityTypeIgnoreCase(List<Long> employeeIds, String activityType, Pageable pageable);
    Page<CustomerActivity> findByEmployeeIdInAndCustomerId(List<Long> employeeIds, Long customerId, Pageable pageable);
    Page<CustomerActivity> findByEmployeeIdInAndCustomerIdAndActivityTypeIgnoreCase(List<Long> employeeIds, Long customerId, String activityType, Pageable pageable);
    List<CustomerActivity> findByEmployeeIdInAndCustomerIdOrderByCreatedAtDesc(List<Long> employeeIds, Long customerId);
}
