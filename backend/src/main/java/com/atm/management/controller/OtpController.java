package com.atm.management.controller;

import com.atm.management.dto.request.OtpInitiateRequest;
import com.atm.management.dto.response.OtpInitiateResponse;
import com.atm.management.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<OtpInitiateResponse> initiate(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody OtpInitiateRequest request) {
        return ResponseEntity.ok(
                otpService.initiate(principal.getUsername(), request.purpose()));
    }
}
