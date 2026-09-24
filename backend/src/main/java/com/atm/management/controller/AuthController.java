package com.atm.management.controller;

import com.atm.management.dto.request.LoginRequest;
import com.atm.management.dto.request.RefreshRequest;
import com.atm.management.dto.request.RegisterRequest;
import com.atm.management.dto.request.ResetPinRequest;
import com.atm.management.dto.response.JwtResponse;
import com.atm.management.dto.response.MessageResponse;
import com.atm.management.dto.response.RefreshResponse;
import com.atm.management.dto.response.RegistrationResponse;
import com.atm.management.dto.response.SecurityQuestionResponse;
import com.atm.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @GetMapping("/security-question/{cardNumber}")
    public ResponseEntity<SecurityQuestionResponse> securityQuestion(
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(authService.getSecurityQuestion(cardNumber));
    }

    @PostMapping("/reset-pin")
    public ResponseEntity<MessageResponse> resetPin(
            @Valid @RequestBody ResetPinRequest request) {
        return ResponseEntity.ok(authService.resetPin(request));
    }
}
