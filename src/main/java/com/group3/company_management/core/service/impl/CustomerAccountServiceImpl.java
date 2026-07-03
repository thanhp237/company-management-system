package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.CustomerAccountResult;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.service.CustomerAccountService;
import com.group3.company_management.core.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 10;

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public CustomerAccountResult createFromContract(Long contractId) {
        Contract contract = getContractReadyForCustomerAccount(contractId);
        Customer customer = getContractCustomer(contract);

        if (hasCustomerAccount(customer)) {
            throw new RuntimeException("Khách hàng đã có tài khoản");
        }

        String rawPassword = generateRandomPassword();
        activateCustomerAccount(customer, rawPassword);
        boolean emailSent = sendEmailSafely(customer, rawPassword);

        return new CustomerAccountResult(emailSent, customer.getEmail(), rawPassword);
    }

    @Override
    @Transactional
    public CustomerAccountResult resendAccountEmail(Long contractId) {
        Contract contract = getContractReadyForCustomerAccount(contractId);
        Customer customer = getContractCustomer(contract);

        if (!hasCustomerAccount(customer)) {
            throw new RuntimeException("Tài khoản khách hàng chưa được tạo");
        }

        String rawPassword = generateRandomPassword();
        customer.setPasswordHash(passwordEncoder.encode(rawPassword));
        customer.setCustomerStatus(ACTIVE_STATUS);
        customerRepository.save(customer);

        boolean emailSent = sendEmailSafely(customer, rawPassword);

        return new CustomerAccountResult(emailSent, customer.getEmail(), rawPassword);
    }

    private Contract getContractReadyForCustomerAccount(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));

        if (contract.getStatus() != Contract.ContractStatus.SENT_TO_CUSTOMER
                && contract.getStatus() != Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Cần gửi hợp đồng cho khách hàng trước khi cấp tài khoản");
        }

        return contract;
    }

    private Customer getContractCustomer(Contract contract) {
        Customer customer = contract.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Hợp đồng chưa có thông tin khách hàng");
        }
        if (!hasText(customer.getEmail())) {
            throw new RuntimeException("Cần có email khách hàng để cấp tài khoản");
        }
        return customer;
    }

    private void activateCustomerAccount(Customer customer, String rawPassword) {
        customer.setPasswordHash(passwordEncoder.encode(rawPassword));
        customer.setCustomerStatus(ACTIVE_STATUS);
        customerRepository.save(customer);
    }

    private boolean hasCustomerAccount(Customer customer) {
        return customer != null && hasText(customer.getPasswordHash());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean sendEmailSafely(Customer customer, String rawPassword) {
        try {
            emailService.sendCustomerAccountEmail(customer.getEmail(), customer.getEmail(), rawPassword);
            return true;
        } catch (MailException exception) {
            return false;
        }
    }

    private String generateRandomPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();


        
    }
}
