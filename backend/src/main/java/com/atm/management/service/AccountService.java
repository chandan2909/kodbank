package com.atm.management.service;

import com.atm.management.entity.Account;
import com.atm.management.entity.TxnEntry;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.AccountRepository;
import com.atm.management.repository.LoginRepository;
import com.atm.management.repository.TxnEntryRepository;
import com.atm.management.dto.response.BalanceResponse;
import com.atm.management.dto.response.StatementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LoginRepository loginRepository;
    private final TxnEntryRepository txnEntryRepository;
    private final TransactionLookupService transactionLookupService;

    public AccountService(
            AccountRepository accountRepository,
            LoginRepository loginRepository,
            TxnEntryRepository txnEntryRepository,
            TransactionLookupService transactionLookupService) {
        this.accountRepository = accountRepository;
        this.loginRepository = loginRepository;
        this.txnEntryRepository = txnEntryRepository;
        this.transactionLookupService = transactionLookupService;
    }

    public Account accountForCard(String cardNo) {
        return loginRepository.findByCardNo(cardNo)
                .flatMap(login -> login.getAccountId() == null
                        ? java.util.Optional.empty()
                        : accountRepository.findById(login.getAccountId()))
                .orElseThrow(() -> new NotFoundException("No bank account is linked to this card."));
    }

    public BalanceResponse getBalance(String cardNumber) {
        Account account = accountForCard(cardNumber);
        return new BalanceResponse(account.getBalance(), "INR");
    }

    public StatementResponse getStatement(
            String cardNumber,
            int page,
            int size,
            LocalDateTime from,
            LocalDateTime to,
            String type) {
        Account account = accountForCard(cardNumber);

        Page<TxnEntry> entryPage = txnEntryRepository.search(
                account.getAccountId(), from, to, normalizeType(type),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)));

        Map<Long, String> typeByTxnId = transactionLookupService.typesFor(
                entryPage.getContent().stream().map(TxnEntry::getTxnId).toList());

        List<StatementResponse.TransactionItem> items = entryPage.getContent().stream()
                .map(e -> toItem(e, typeByTxnId.get(e.getTxnId())))
                .toList();

        return new StatementResponse(
                maskCardNumber(cardNumber),
                account.getBalance(),
                items,
                entryPage.getNumber(),
                entryPage.getSize(),
                entryPage.getTotalElements(),
                entryPage.getTotalPages());
    }

    public StatementResponse getFullStatement(
            String cardNumber,
            LocalDateTime from,
            LocalDateTime to,
            String type) {
        Account account = accountForCard(cardNumber);
        List<TxnEntry> entries = txnEntryRepository.findAllForStatement(
                account.getAccountId(), from, to, normalizeType(type));
        Map<Long, String> typeByTxnId = transactionLookupService.typesFor(
                entries.stream().map(TxnEntry::getTxnId).toList());
        List<StatementResponse.TransactionItem> items = entries.stream()
                .map(e -> toItem(e, typeByTxnId.get(e.getTxnId())))
                .toList();
        return new StatementResponse(
                maskCardNumber(cardNumber),
                account.getBalance(),
                items,
                0,
                items.size(),
                items.size(),
                items.size() > 0 ? 1 : 0);
    }

    private StatementResponse.TransactionItem toItem(TxnEntry entry, String txnType) {
        String type = displayType(txnType, entry.getDirection());
        return new StatementResponse.TransactionItem(
                entry.getCreatedAt() != null ? entry.getCreatedAt() : LocalDateTime.now(),
                type,
                entry.getAmount(),
                entry.getDirection());
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

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "XXXXXXXX" + cardNumber.substring(12);
    }

    public BigDecimal balanceOf(String cardNumber) {
        return accountForCard(cardNumber).getBalance();
    }

    public void createAccount(String formNo, String accountType, BigDecimal openingBalance,
                              BigDecimal dailyLimit) {
        Account account = new Account();
        account.setAccountNo(generateAccountNo());
        account.setFormNo(formNo);
        account.setAccountType(accountType == null || accountType.isBlank() ? "Saving" : accountType);
        account.setBalance(openingBalance == null ? BigDecimal.ZERO : openingBalance);
        account.setDailyLimit(dailyLimit == null ? BigDecimal.valueOf(50000) : dailyLimit);
        account.setStatus(Account.STATUS_ACTIVE);
        account.setSystem(0);
        account.setCreatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    public String generateAccountNo() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String accountNo;
        do {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            accountNo = sb.toString();
        } while (accountRepository.existsByAccountNo(accountNo));
        return accountNo;
    }

    public Account createAccountReturning(String formNo, String accountType,
                                          BigDecimal openingBalance, BigDecimal dailyLimit) {
        Account account = new Account();
        account.setAccountNo(generateAccountNo());
        account.setFormNo(formNo);
        account.setAccountType(accountType == null || accountType.isBlank() ? "Saving" : accountType);
        account.setBalance(openingBalance == null ? BigDecimal.ZERO : openingBalance);
        account.setDailyLimit(dailyLimit == null ? BigDecimal.valueOf(50000) : dailyLimit);
        account.setStatus(Account.STATUS_ACTIVE);
        account.setSystem(0);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    public void linkLoginToAccount(String formNo, String cardNo) {
        loginRepository.findByCardNo(cardNo).ifPresent(login -> {
            accountRepository.findByFormNo(formNo).ifPresent(account -> {
                login.setAccountId(account.getAccountId());
                loginRepository.save(login);
            });
        });
    }

    public void assertWithinMaxWithdrawal(BigDecimal amount, BigDecimal maxWithdrawal) {
        if (amount.compareTo(maxWithdrawal) > 0) {
            throw new BusinessRuleException(
                    "Maximum withdrawal is Rs." + maxWithdrawal.stripTrailingZeros().toPlainString());
        }
    }
}
