package com.atm.management.repository;

import com.atm.management.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findByCardNoAndPurposeOrderByCreatedAtDesc(String cardNo, String purpose);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
