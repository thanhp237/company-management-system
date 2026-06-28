package com.group3.company_management.core.service;



import com.group3.company_management.core.entity.CustomerActivity;
import org.springframework.data.domain.Page;

import java.util.List;


public interface CustomerActivityService {


    
    Page<CustomerActivity> getActivities(
            String activityType,
            int page,
            int size);

    CustomerActivity getActivityById(Long id);

    List<CustomerActivity> getActivitiesByCustomerId(Long customerId);
    void updateActivityNote(Long id, String note);
    

    CustomerActivity save(CustomerActivity activity);

    void delete(Long id);
}
