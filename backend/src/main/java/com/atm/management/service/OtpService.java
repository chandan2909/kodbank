package com.atm.management.service;

import com.atm.management.dto.response.OtpInitiateResponse;
import com.atm.management.entity.OtpCode;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.exception.InvalidCredentialsException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.OtpCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class OtpService {

    public static final String PURPOSE_WITHDRAW = "WITHDRAW";
    public static final String PURPOSE_TRANSFER = "TRANSFER";

    private final OtpCodeRepository otpCodeRepository;
    private final long ttlSeconds;
    private final boolean includeCode;
    private final SecureRandom random = new SecureRandom();

    public OtpService(
            OtpCodeRepository otpCodeRepository,
            @Value("${app.otp.ttl-seconds}") long ttlSeconds,
            @Value("${app.otp.include-code:true}") boolean includeCode) {
        this.otpCodeRepository = otpCodeRepository;
        this.ttlSeconds = ttlSeconds;
        this.includeCode = includeCode;
    }

    @Transactional
    public OtpInitiateResponse initiate(String cardNo, String purpose) {
        if (!PURPOSE_WITHDRAW.equals(purpose) && !PURPOSE_TRANSFER.equals(purpose)) {
            throw new BusinessRuleException("Unsupported OTP purpose");
        }

        String raw = String.format("%06d", random.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setCardNo(cardNo);
        otp.setPurpose(purpose);
        otp.setCodeHash(sha256(raw));
        otp.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        otp.setUsed(0);
        otp.setCreatedAt(LocalDateTime.now());
        otpCodeRepository.save(otp);

        return new OtpInitiateResponse(
                String.valueOf(otp.getId()),
                purpose,
                ttlSeconds,
                includeCode
                        ? "OTP created for this session"
                        : "OTP sent to your registered contact",
                includeCode ? raw : null);
    }

    @Transactional
    public void verify(String cardNo, String purpose, String otpId, String code) {
        if (otpId == null || otpId.isBlank() || code == null || code.isBlank()) {
            throw new BusinessRuleException("OTP is required for this transaction");
        }

        OtpCode otp;
        try {
            otp = otpCodeRepository.findById(Long.parseLong(otpId))
                    .orElseThrow(() -> new NotFoundException("OTP not found"));
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Invalid OTP request");
        }

        if (!otp.getCardNo().equals(cardNo) || !otp.getPurpose().equals(purpose)) {
            throw new BusinessRuleException("OTP does not match this transaction");
        }
        if (otp.isUsed()) {
            throw new BusinessRuleException("OTP has already been used");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("OTP has expired. Please request a new one.");
        }
        if (!otp.getCodeHash().equals(sha256(code.trim()))) {
            throw new InvalidCredentialsException("Incorrect OTP");
        }

        otp.setUsed(1);
        otpCodeRepository.save(otp);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
