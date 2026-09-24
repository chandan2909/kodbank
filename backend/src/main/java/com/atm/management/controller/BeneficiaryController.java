package com.atm.management.controller;

import com.atm.management.dto.request.BeneficiaryRequest;
import com.atm.management.dto.response.BeneficiaryResponse;
import com.atm.management.dto.response.MessageResponse;
import com.atm.management.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> list(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(beneficiaryService.list(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> add(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody BeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.add(principal.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> remove(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        beneficiaryService.remove(principal.getUsername(), id);
        return ResponseEntity.ok(new MessageResponse("Beneficiary removed"));
    }
}
