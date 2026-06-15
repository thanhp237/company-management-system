package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.LeadDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CustomerImportService {
    List<LeadDTO> importCustomer(MultipartFile file);
}
