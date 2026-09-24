package com.atm.management.controller;

import com.atm.management.dto.request.LimitRequest;
import com.atm.management.dto.response.AdminAccountResponse;
import com.atm.management.dto.response.AuditResponse;
import com.atm.management.dto.response.MessageResponse;
import com.atm.management.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AdminAccountResponse>> accounts() {
        return ResponseEntity.ok(adminService.listAccounts());
    }

    @PostMapping("/accounts/{id}/unlock")
    public ResponseEntity<MessageResponse> unlock(@PathVariable Long id) {
        adminService.unlock(id);
        return ResponseEntity.ok(new MessageResponse("Account unlocked"));
    }

    @PostMapping("/accounts/{id}/freeze")
    public ResponseEntity<AdminAccountResponse> freeze(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.freeze(id));
    }

    @PostMapping("/accounts/{id}/unfreeze")
    public ResponseEntity<AdminAccountResponse> unfreeze(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unfreeze(id));
    }

    @PutMapping("/accounts/{id}/limit")
    public ResponseEntity<AdminAccountResponse> setLimit(
            @PathVariable Long id,
            @Valid @RequestBody LimitRequest request) {
        return ResponseEntity.ok(adminService.setLimit(id, request.dailyLimit()));
    }

    @GetMapping("/audit")
    public ResponseEntity<AuditResponse> audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(adminService.getAudit(
                page, size, parseStart(from), parseEnd(to), type));
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
