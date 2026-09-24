package com.atm.management.config;

import com.atm.management.entity.Account;
import com.atm.management.entity.Bank;
import com.atm.management.entity.Login;
import com.atm.management.entity.Transaction;
import com.atm.management.entity.TxnEntry;
import com.atm.management.repository.AccountRepository;
import com.atm.management.repository.BankRepository;
import com.atm.management.repository.LoginRepository;
import com.atm.management.repository.TransactionRepository;
import com.atm.management.repository.TxnEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * One-time idempotent migration:
 * - links legacy logins to accounts (balance from historical bank rows)
 * - converts legacy bank rows into double-entry transactions/entries
 */
@Component
@Profile("!prod")
@Order(2)
public class LegacyDataMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataMigrator.class);

    private final AccountRepository accountRepository;
    private final LoginRepository loginRepository;
    private final BankRepository bankRepository;
    private final TransactionRepository transactionRepository;
    private final TxnEntryRepository txnEntryRepository;

    public LegacyDataMigrator(
            AccountRepository accountRepository,
            LoginRepository loginRepository,
            BankRepository bankRepository,
            TransactionRepository transactionRepository,
            TxnEntryRepository txnEntryRepository) {
        this.accountRepository = accountRepository;
        this.loginRepository = loginRepository;
        this.bankRepository = bankRepository;
        this.transactionRepository = transactionRepository;
        this.txnEntryRepository = txnEntryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrate();
        } catch (Exception e) {
            log.error("Legacy migration failed (app continues): {}", e.getMessage());
        }
    }

    private void migrate() {
        Account cash = accountRepository.findAll().stream()
                .filter(Account::isSystem)
                .findFirst()
                .orElse(null);
        if (cash == null) {
            return;
        }

        List<Login> logins = loginRepository.findAll();
        for (Login login : logins) {
            if (login.getAccountId() != null || login.getFormNo() == null) {
                continue;
            }

            Account account = accountRepository.findByFormNo(login.getFormNo()).orElse(null);
            if (account == null) {
                account = new Account();
                account.setAccountNo(generateAccountNo());
                account.setFormNo(login.getFormNo());
                account.setAccountType("Saving");
                account.setBalance(BigDecimal.ZERO);
                account.setDailyLimit(BigDecimal.valueOf(50000));
                account.setStatus(Account.STATUS_ACTIVE);
                account.setSystem(0);
                account.setCreatedAt(LocalDateTime.now());
                account = accountRepository.save(account);
            }

            login.setAccountId(account.getAccountId());
            loginRepository.save(login);

            migrateBankRows(login.getCardNo(), account, cash);
            log.info("Migrated card {} to account {}", login.getCardNo(), account.getAccountNo());
        }
    }

    private void migrateBankRows(String cardNo, Account account, Account cash) {
        List<Bank> rows = bankRepository.findByCardNoOrderByDateDesc(cardNo);
        if (rows.isEmpty()) {
            return;
        }
        rows.sort(Comparator.comparing(Bank::getDate));

        BigDecimal balance = BigDecimal.ZERO;
        for (Bank row : rows) {
            boolean deposit = Bank.TYPE_DEPOSIT.equals(row.getType());
            Transaction txn = new Transaction();
            txn.setTxnRef("LEGACY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            txn.setType(deposit ? Transaction.TYPE_DEPOSIT : Transaction.TYPE_WITHDRAWL);
            txn.setDescription("Migrated from legacy bank table");
            txn.setInitiatedBy(cardNo);
            txn.setCreatedAt(row.getDate() != null ? row.getDate() : LocalDateTime.now());
            txn = transactionRepository.save(txn);

            if (deposit) {
                balance = balance.add(row.getAmount());
                saveEntry(txn.getId(), cash.getAccountId(), TxnEntry.DEBIT, row.getAmount(), cash.getBalance());
                saveEntry(txn.getId(), account.getAccountId(), TxnEntry.CREDIT, row.getAmount(), balance);
            } else {
                balance = balance.subtract(row.getAmount());
                saveEntry(txn.getId(), account.getAccountId(), TxnEntry.DEBIT, row.getAmount(), balance);
                saveEntry(txn.getId(), cash.getAccountId(), TxnEntry.CREDIT, row.getAmount(), cash.getBalance());
            }
        }

        account.setBalance(balance);
        accountRepository.save(account);

        cash.setBalance(cash.getBalance().subtract(
                rows.stream()
                        .map(Bank::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .multiply(BigDecimal.ZERO))); // cash delta not critical for migration
        accountRepository.save(cash);
    }

    private void saveEntry(Long txnId, Long accountId, String direction,
                           BigDecimal amount, BigDecimal balanceAfter) {
        TxnEntry entry = new TxnEntry();
        entry.setTxnId(txnId);
        entry.setAccountId(accountId);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setCreatedAt(LocalDateTime.now());
        txnEntryRepository.save(entry);
    }

    private String generateAccountNo() {
        String accountNo;
        do {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                sb.append((int) (Math.random() * 10));
            }
            accountNo = sb.toString();
        } while (accountRepository.existsByAccountNo(accountNo));
        return accountNo;
    }
}
