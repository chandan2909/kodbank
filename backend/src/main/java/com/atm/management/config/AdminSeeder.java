package com.atm.management.config;

import com.atm.management.entity.Account;
import com.atm.management.entity.Login;
import com.atm.management.repository.AccountRepository;
import com.atm.management.repository.LoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(1)
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AccountRepository accountRepository;
    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;
    private final BigDecimal dailyLimit;
    private final String adminUsername;
    private final String adminPin;
    private final String adminCard;
    private final String adminSecurityAnswer;

    public AdminSeeder(
            AccountRepository accountRepository,
            LoginRepository loginRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.account.daily-limit}") long dailyLimit,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.pin}") String adminPin,
            @Value("${app.admin.card}") String adminCard,
            @Value("${app.admin.security-answer}") String adminSecurityAnswer) {
        this.accountRepository = accountRepository;
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
        this.dailyLimit = BigDecimal.valueOf(dailyLimit);
        this.adminUsername = adminUsername;
        this.adminPin = adminPin;
        this.adminCard = adminCard;
        this.adminSecurityAnswer = adminSecurityAnswer;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureCashAccount();
            ensureAdmin();
        } catch (Exception e) {
            log.error("Admin/cash seed failed (app continues): {}", e.getMessage());
        }
    }

    private void ensureCashAccount() {
        boolean cashExists = accountRepository.findAll().stream()
                .anyMatch(Account::isSystem);
        if (cashExists) {
            return;
        }
        Account cash = new Account();
        cash.setAccountNo("CASH0000000");
        cash.setFormNo(null);
        cash.setAccountType("SYSTEM");
        cash.setBalance(new BigDecimal("5000000"));
        cash.setDailyLimit(new BigDecimal("9999999999999.99"));
        cash.setStatus(Account.STATUS_ACTIVE);
        cash.setSystem(1);
        cash.setCreatedAt(LocalDateTime.now());
        accountRepository.save(cash);
        log.info("Seeded system CASH account");
    }

    private void ensureAdmin() {
        var existing = loginRepository.findById(adminCard);
        if (existing.isPresent()) {
            Login login = existing.get();
            if (login.getUsername() == null || login.getUsername().isBlank()) {
                login.setUsername(adminUsername);
                loginRepository.save(login);
                log.info("Backfilled ADMIN username={}", adminUsername);
            }
            return;
        }
        Account adminAccount = accountRepository.findByAccountNo("9999000001")
                .orElseGet(() -> {
                    Account a = new Account();
                    a.setAccountNo("9999000001");
                    a.setFormNo(null);
                    a.setAccountType("ADMIN");
                    a.setBalance(BigDecimal.ZERO);
                    a.setDailyLimit(dailyLimit);
                    a.setStatus(Account.STATUS_ACTIVE);
                    a.setSystem(0);
                    a.setCreatedAt(LocalDateTime.now());
                    return accountRepository.save(a);
                });

        Login login = new Login();
        login.setCardNo(adminCard);
        login.setUsername(adminUsername);
        login.setFormNo(null);
        login.setPin(passwordEncoder.encode(adminPin));
        login.setFailedAttempts(0);
        login.setLocked(0);
        login.setRole("ADMIN");
        login.setAccountId(adminAccount.getAccountId());
        login.setSecurityQuestion("What is the name of this bank system?");
        login.setSecurityAnswer(passwordEncoder.encode(adminSecurityAnswer));
        login.setCreatedAt(LocalDateTime.now());
        loginRepository.save(login);

        log.info("Seeded ADMIN login — username={}, card=****{}", adminUsername, maskTail(adminCard));
    }

    private String maskTail(String card) {
        if (card == null || card.length() < 4) {
            return "****";
        }
        return card.substring(card.length() - 4);
    }
}
