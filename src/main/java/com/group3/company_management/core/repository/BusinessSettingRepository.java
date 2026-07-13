package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.BusinessSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingRepository extends JpaRepository<BusinessSetting, String> {
}
