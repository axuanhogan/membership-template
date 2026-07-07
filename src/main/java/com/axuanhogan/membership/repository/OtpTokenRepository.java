package com.axuanhogan.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.axuanhogan.membership.dao.OtpToken;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(String email, String otpCode);
}
