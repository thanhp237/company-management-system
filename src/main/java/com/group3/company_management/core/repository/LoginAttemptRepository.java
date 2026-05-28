package com.group3.company_management.core.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group3.company_management.core.entity.LoginAttempt;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    /**
     * Find recent failed login attempts for a user
     * Used to count failed attempts for account locking
     */
    List<LoginAttempt> findByUserIdAndAttemptAtAfter(Long userId, LocalDateTime after);
    
    /**
     * Find all failed attempts by username in a time window
     */
    List<LoginAttempt> findByUsernameAndStatusAndAttemptAtAfter(
            String username, String status, LocalDateTime after);
}