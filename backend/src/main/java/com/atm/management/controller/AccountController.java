package com.atm.management.controller;

import com.atm.management.dto.request.ChangePinRequest;
import com.atm.management.dto.response.BalanceResponse;
import com.atm.management.dto.response.MessageResponse;
import com.atm.management.dto.response.StatementResponse;
import com.atm.management.entity.Account;
import com.atm.management.service.AccountService;
import com.atm.management.service.AuthService;
import com.atm.management.service.StatementPdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;
    private final StatementPdfService statementPdfService;

    public AccountController(
            AccountService accountService,
            AuthService authService,
            StatementPdfService statementPdfService) {
        this.accountService = accountService;
        this.authService = authService;
        this.statementPdfService = statementPdfService;
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> balance(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(accountService.getBalance(principal.getUsername()));
    }

    @GetMapping("/statement")
    public ResponseEntity<StatementResponse> statement(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(accountService.getStatement(
                principal.getUsername(), page, size,
                parseStart(from), parseEnd(to), type));
    }

    @GetMapping("/statement.pdf")
    public ResponseEntity<byte[]> statementPdf(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type) {
        StatementResponse statement = accountService.getFullStatement(
                principal.getUsername(), parseStart(from), parseEnd(to), type);
        Account account = accountService.accountForCard(principal.getUsername());
        byte[] pdf = statementPdfService.render(statement, account.getAccountNo());
        String filename = "statement-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/change-pin")
    public ResponseEntity<MessageResponse> changePin(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePinRequest request) {
        return ResponseEntity.ok(authService.changePin(
                principal.getUsername(), request.newPin(), request.confirmPin()));
    }

    private LocalDateTime parseStart(String from) {
        if (from == null || from.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(from).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime parseEnd(String to) {
        if (to == null || to.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(to).plusDays(1).atStartOfDay().minusNanos(1);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
