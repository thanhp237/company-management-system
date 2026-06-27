package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.CustomerAccountResult;

public interface CustomerAccountService {

    CustomerAccountResult createFromContract(Long contractId);

    CustomerAccountResult resendAccountEmail(Long contractId);
}
