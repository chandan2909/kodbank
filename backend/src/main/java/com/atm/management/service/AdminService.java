package com.atm.management.service;

import com.atm.management.dto.response.AdminAccountResponse;
import com.atm.management.dto.response.AuditResponse;
import com.atm.management.entity.Account;
import com.atm.management.entity.Login;
import com.atm.management.entity.TxnEntry;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.AccountRepository;
import com.atm.management.repository.LoginRepository;
import com.atm.management.repository.SignupRepository;
import com.atm.management.repository.TransactionRepository;
import com.atm.management.repository.TxnEntryRepository;
import com.atm.management.entity.Signup;
import com.atm.management.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final AccountRepository accountRepository;
    private final LoginRepository loginRepository;
    private final SignupRepository signupRepository;
    private final AccountSecurityService accountSecurityService;
    private final TxnEntryRepository txnEntryRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(
            AccountRepository accountRepository,
            LoginRepository loginRepository,
            SignupRepository signupRepository,
            AccountSecurityService accountSecurityService,
            TxnEntryRepository txnEntryRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.loginRepository = loginRepository;
        this.signupRepository = signupRepository;
        this.accountSecurityService = accountSecurityService;
        this.txnEntryRepository = txnEntryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<AdminAccountResponse> listAccounts() {
        List<Account> accounts = accountRepository.findAllByOrderByAccountIdAsc();
        List<Login> logins = loginRepository.findAll();
        Map<Long, Login> byAccountId = logins.stream()
                .filter(l -> l.getAccountId() != null)
                .collect(Collectors.toMap(Login::getAccountId, l -> l, (a, b) -> a));
        Map<String, List<Login>> byFormNo = logins.stream()
                .filter(l -> l.getFormNo() != null)
                .collect(Collectors.groupingBy(Login::getFormNo));

        List<AdminAccountResponse> result = new ArrayList<>();
        for (Account a : accounts) {
            if (a.isSystem()) {
                continue;
            }
            Login login = byAccountId.get(a.getAccountId());
            if (login == null && a.getFormNo() != null) {
                login = byFormNo.getOrDefault(a.getFormNo(), List.of()).stream()
                        .findFirst()
                        .orElse(null);
            }
            final Login matchedLogin = login;
            String holder = Optional.ofNullable(a.getFormNo())
                    .flatMap(f -> signupRepository.findById(f))
                    .map(Signup::getName)
                    .orElseGet(() -> {
                        if (matchedLogin != null && "ADMIN".equalsIgnoreCase(matchedLogin.getRole())) {
                            return matchedLogin.getUsername() != null
                                    ? matchedLogin.getUsername()
                                    : "Administrator";
                        }
                        return "—";
                    });
            result.add(new AdminAccountResponse(
                    a.getAccountId(),
                    a.getAccountNo(),
                    holder,
                    a.getBalance(),
                    a.getDailyLimit(),
                    a.getStatus(),
                    matchedLogin != null ? maskCardNumber(matchedLogin.getCardNo()) : null,
                    matchedLogin != null ? matchedLogin.getFailedAttempts() : 0,
                    matchedLogin != null && matchedLogin.isLocked()));
        }
        return result;
    }

    @Transactional
    public void unlock(Long accountId) {
        Account account = require(accountId);
        loginRepository.findAll().stream()
                .filter(l -> account.getAccountId().equals(l.getAccountId()))
                .forEach(l -> accountSecurityService.unlock(l.getCardNo()));
    }

    @Transactional
    public AdminAccountResponse freeze(Long accountId) {
        Account account = require(accountId);
        account.setStatus(Account.STATUS_FROZEN);
        accountRepository.save(account);
        return toResponse(account);
    }

    @Transactional
    public AdminAccountResponse unfreeze(Long accountId) {
        Account account = require(accountId);
        account.setStatus(Account.STATUS_ACTIVE);
        accountRepository.save(account);
        return toResponse(account);
    }

    @Transactional
    public AdminAccountResponse setLimit(Long accountId, BigDecimal dailyLimit) {
        if (dailyLimit == null || dailyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Daily limit must be greater than zero");
        }
        Account account = require(accountId);
        account.setDailyLimit(dailyLimit);
        accountRepository.save(account);
        return toResponse(account);
    }

    public AuditResponse getAudit(
            int page,
            int size,
            LocalDateTime from,
            LocalDateTime to,
            String type) {
        Page<TxnEntry> entryPage = txnEntryRepository.searchAll(
                from, to, normalizeType(type),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)));

        Set<Long> txnIds = entryPage.getContent().stream()
                .map(TxnEntry::getTxnId)
                .collect(Collectors.toSet());
        Set<Long> accountIds = entryPage.getContent().stream()
                .map(TxnEntry::getAccountId)
                .collect(Collectors.toSet());

        Map<Long, Transaction> txns = transactionRepository.findAllById(txnIds).stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity(), (a, b) -> a));
        Map<Long, Account> accounts = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getAccountId, Function.identity(), (a, b) -> a));
        Map<Long, Login> loginsByAccount = loginRepository.findAll().stream()
                .filter(l -> l.getAccountId() != null)
                .collect(Collectors.toMap(Login::getAccountId, Function.identity(), (a, b) -> a));
        Set<String> formNos = accounts.values().stream()
                .map(Account::getFormNo)
                .filter(f -> f != null && !f.isBlank())
                .collect(Collectors.toSet());
        Map<String, Signup> signups = signupRepository.findAllById(formNos).stream()
                .collect(Collectors.toMap(Signup::getFormNo, Function.identity(), (a, b) -> a));

        List<AuditResponse.AuditItem> items = entryPage.getContent().stream()
                .map(e -> {
                    Transaction txn = txns.get(e.getTxnId());
                    Account account = accounts.get(e.getAccountId());
                    Login login = loginsByAccount.get(e.getAccountId());
                    Signup signup = account != null && account.getFormNo() != null
                            ? signups.get(account.getFormNo())
                            : null;
                    String typeLabel = displayType(
                            txn != null ? txn.getType() : null,
                            e.getDirection());
                    String description = txn != null
                            ? (txn.getDescription() != null && !txn.getDescription().isBlank()
                                    ? txn.getDescription()
                                    : txn.getTxnRef())
                            : null;
                    String initiatedBy = txn != null ? maskCardNumber(txn.getInitiatedBy()) : null;
                    LocalDateTime date = e.getCreatedAt() != null
                            ? e.getCreatedAt()
                            : (txn != null && txn.getCreatedAt() != null
                                    ? txn.getCreatedAt()
                                    : LocalDateTime.now());
                    return new AuditResponse.AuditItem(
                            e.getId(),
                            date,
                            typeLabel,
                            e.getDirection(),
                            e.getAmount(),
                            e.getBalanceAfter(),
                            account != null ? account.getAccountNo() : "—",
                            signup != null ? signup.getName() : "—",
                            login != null ? maskCardNumber(login.getCardNo()) : "—",
                            description,
                            initiatedBy);
                })
                .toList();

        return new AuditResponse(
                items,
                entryPage.getNumber(),
                entryPage.getSize(),
                entryPage.getTotalElements(),
                entryPage.getTotalPages());
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type.trim().toUpperCase()) {
            case "DEPOSIT" -> "DEPOSIT";
            case "WITHDRAWL", "WITHDRAW", "WITHDRAWAL" -> "WITHDRAWL";
            case "TRANSFER" -> "TRANSFER";
            default -> type.trim().toUpperCase();
        };
    }

    private String displayType(String txnType, String direction) {
        if (txnType == null) {
            return TxnEntry.CREDIT.equals(direction) ? "Deposit" : "Withdrawl";
        }
        return switch (txnType) {
            case "DEPOSIT" -> "Deposit";
            case "WITHDRAWL" -> "Withdrawl";
            case "TRANSFER" -> TxnEntry.CREDIT.equals(direction) ? "Transfer In" : "Transfer Out";
            default -> txnType;
        };
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "XXXXXXXX" + cardNumber.substring(12);
    }

    private Account require(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    private AdminAccountResponse toResponse(Account a) {
        return new AdminAccountResponse(
                a.getAccountId(),
                a.getAccountNo(),
                "—",
                a.getBalance(),
                a.getDailyLimit(),
                a.getStatus(),
                null,
                0,
                false);
    }
}
