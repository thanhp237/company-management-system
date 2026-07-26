package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    @Query("""
            select p from PasswordResetOtp p
            where lower(p.email) = lower(:email)
            and p.otpCode = :otpCode
            and p.used = false
            and p.expiresAt > :now
            order by p.createdAt desc
            """)
    Optional<PasswordResetOtp> findValidOtp(
            @Param("email") String email,
            @Param("otpCode") String otpCode,
            @Param("now") LocalDateTime now);

    Optional<PasswordResetOtp> findByEmailAndResetTokenAndUsedFalse(String email, String resetToken);
}
