// package com.group3.company_management.customer.security;

// // src/main/java/com/group3/company_management/customer/security/CustomerUserDetailsService.java


// import com.group3.company_management.core.entity.Customer;
// import com.group3.company_management.core.repository.CustomerRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// /**
//  * Spring Security 6+ UserDetailsService for Customers
//  * Loads customer by email for login
//  */
// @Service("customerUserDetailsService")
// @RequiredArgsConstructor
// @Slf4j
// public class CustomerUserDetailsService implements UserDetailsService {
    
//     private final CustomerRepository customerRepository;
    
//     @Override
//     @Transactional(readOnly = true)
//     public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//         Customer customer = customerRepository.findByEmailAndNotDeleted(email)
//                 .orElseThrow(() -> {
//                     log.warn("❌ Customer not found: {}", email);
//                     return new UsernameNotFoundException("Customer not found: " + email);
//                 });
        
//         // Validate customer account status
//         if (!customer.isEnabled()) {
//             log.warn("❌ Customer account is INACTIVE: {}", email);
//             throw new UsernameNotFoundException("Customer account is inactive");
//         }
        
//         if (!customer.isAccountNonLocked()) {
//             log.warn("❌ Customer account is LOCKED: {}", email);
//             throw new UsernameNotFoundException("Customer account is locked. Please contact support.");
//         }
        
//         log.info("✅ Customer loaded successfully: {}", email);
//         return customer; // Customer implements UserDetails
//     }
// }