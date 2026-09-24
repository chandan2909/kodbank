package com.atm.management.service;

import com.atm.management.entity.RefreshToken;
import com.atm.management.exception.InvalidCredentialsException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public String issue(String cardNo) {
        String raw = UUID.randomUUID() + "-" + UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setCardNo(cardNo);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(LocalDateTime.now().plusSeconds(Math.max(1, refreshExpirationMs / 1000)));
        token.setRevoked(0);
        token.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        return raw;
    }

    @Transactional
    public String rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException("Refresh token is required");
        }
        RefreshToken existing = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (existing.isRevoked()) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }
        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        existing.setRevoked(1);
        refreshTokenRepository.save(existing);
        return existing.getCardNo();
    }

    public String cardFor(String rawToken) {
        return refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(RefreshToken::getCardNo)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
