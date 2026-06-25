package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.repository.CustomerActivityRepository;
import com.group3.company_management.core.service.CustomerActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerActivityServiceImpl
        implements CustomerActivityService {

    private final CustomerActivityRepository repository;

    @Override
    public Page<CustomerActivity> getActivities(
            String activityType,
            int page,
            int size) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt"));

        if (activityType == null || activityType.isBlank()) {
            return repository.findAll(pageable);
        }

        return repository.findByActivityTypeIgnoreCase(
                activityType,
                pageable);
    }

    @Override
    public CustomerActivity getActivityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Activity not found."));
    }

    @Override
    public List<CustomerActivity> getActivitiesByCustomerId(
            Long customerId) {

        return repository
                .findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public CustomerActivity save(CustomerActivity activity) {
        return repository.save(activity);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}