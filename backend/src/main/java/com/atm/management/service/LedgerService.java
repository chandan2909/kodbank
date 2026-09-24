package com.atm.management.service;

import com.atm.management.entity.Account;
import com.atm.management.entity.Login;
import com.atm.management.entity.Transaction;
import com.atm.management.entity.TxnEntry;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.AccountRepository;
import com.atm.management.repository.LoginRepository;
import com.atm.management.repository.TransactionRepository;
import com.atm.management.repository.TxnEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Double-entry ledger core. Every money movement posts balanced legs
 * under a single transaction header, locking accounts in ascending
 * accountId order to avoid deadlocks.
 */
@Service
public class LedgerService {

    public record Leg(long accountId, String direction, BigDecimal amount) {

        public static Leg debit(long accountId, BigDecimal amount) {
            return new Leg(accountId, TxnEntry.DEBIT, amount);
        }

        public static Leg credit(long accountId, BigDecimal amount) {
            return new Leg(accountId, TxnEntry.CREDIT, amount);
        }
    }

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TxnEntryRepository txnEntryRepository;
    private final LoginRepository loginRepository;
    private final SecureRandom random = new SecureRandom();

    public LedgerService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TxnEntryRepository txnEntryRepository,
            LoginRepository loginRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.txnEntryRepository = txnEntryRepository;
        this.loginRepository = loginRepository;
    }

    @Transactional
    public Account requireAccountByCard(String cardNo) {
        Login login = loginRepository.findByCardNo(cardNo)
                .orElseThrow(() -> new NotFoundException("Account not found for this card."));
        if (login.getAccountId() == null) {
            throw new NotFoundException("No bank account is linked to this card.");
        }
        return accountRepository.findByIdForUpdate(login.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    @Transactional
    public Account requireAccountByNo(String accountNo) {
        return accountRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new NotFoundException("Beneficiary account not found."));
    }

    public Account requireSystemCashAccount() {
        return accountRepository.findAllByOrderByAccountIdAsc().stream()
                .filter(Account::isSystem)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("System cash account is missing"));
    }

    public void assertActive(Account account) {
        if (account.isSystem()) {
            return;
        }
        if (!account.isActive()) {
            throw new BusinessRuleException(
                    "Account is " + account.getStatus().toLowerCase()
                            + " and cannot perform this operation");
        }
    }

    public void assertSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient Balance");
        }
    }

    public void assertDailyLimit(Account account, BigDecimal amount) {
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        BigDecimal used = txnEntryRepository.sumDebitsSince(account.getAccountId(), dayStart);
        BigDecimal total = (used == null ? BigDecimal.ZERO : used).add(amount);
        if (total.compareTo(account.getDailyLimit()) > 0) {
            throw new BusinessRuleException(
                    "Daily limit exceeded. Limit is Rs."
                            + account.getDailyLimit().stripTrailingZeros().toPlainString());
        }
    }

    /**
     * Posts a balanced double-entry transaction. Caller must have already
     * validated amounts and (if needed) OTP. Accounts are locked here in
     * ascending accountId order; balances and balance_after snapshots are
     * written atomically.
     */
    @Transactional
    public BigDecimal post(String type, String description, String initiatedBy, List<Leg> legs) {
        if (legs.isEmpty()) {
            throw new BusinessRuleException("Transaction has no legs");
        }
        BigDecimal debitTotal = legs.stream()
                .filter(l -> TxnEntry.DEBIT.equals(l.direction()))
                .map(Leg::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = legs.stream()
                .filter(l -> TxnEntry.CREDIT.equals(l.direction()))
                .map(Leg::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new BusinessRuleException("Transaction legs are unbalanced");
        }

        List<Leg> ordered = new ArrayList<>(legs);
        ordered.sort(Comparator.comparingLong(Leg::accountId));

        Transaction txn = new Transaction();
        txn.setTxnRef(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        txn.setType(type);
        txn.setDescription(description);
        txn.setInitiatedBy(initiatedBy);
        txn.setCreatedAt(LocalDateTime.now());
        txn = transactionRepository.save(txn);

        BigDecimal primaryNewBalance = null;
        long primaryAccountId = ordered.get(0).accountId();

        for (Leg leg : ordered) {
            Account account = accountRepository.findByIdForUpdate(leg.accountId())
                    .orElseThrow(() -> new NotFoundException("Account not found: " + leg.accountId()));

            if (TxnEntry.DEBIT.equals(leg.direction())) {
                if (account.getBalance().compareTo(leg.amount()) < 0) {
                    throw new BusinessRuleException("Insufficient Balance");
                }
                account.setBalance(account.getBalance().subtract(leg.amount()));
            } else {
                account.setBalance(account.getBalance().add(leg.amount()));
            }
            accountRepository.save(account);

            TxnEntry entry = new TxnEntry();
            entry.setTxnId(txn.getId());
            entry.setAccountId(leg.accountId());
            entry.setDirection(leg.direction());
            entry.setAmount(leg.amount());
            entry.setBalanceAfter(account.getBalance());
            entry.setCreatedAt(LocalDateTime.now());
            txnEntryRepository.save(entry);

            if (leg.accountId() == primaryAccountId) {
                primaryNewBalance = account.getBalance();
            }
        }

        return primaryNewBalance != null ? primaryNewBalance : BigDecimal.ZERO;
    }
}
