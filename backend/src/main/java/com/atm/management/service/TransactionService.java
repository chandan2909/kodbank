package com.atm.management.service;

import com.atm.management.dto.request.TransactionRequest;
import com.atm.management.dto.response.TransactionResponse;
import com.atm.management.entity.Account;
import com.atm.management.entity.Transaction;
import com.atm.management.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final LedgerService ledgerService;
    private final OtpService otpService;
    private final BigDecimal maxWithdrawal;
    private final long otpThreshold;

    public TransactionService(
            LedgerService ledgerService,
            OtpService otpService,
            @Value("${app.security.max-withdrawal}") long maxWithdrawal,
            @Value("${app.otp.withdraw-threshold}") long otpThreshold) {
        this.ledgerService = ledgerService;
        this.otpService = otpService;
        this.maxWithdrawal = BigDecimal.valueOf(maxWithdrawal);
        this.otpThreshold = otpThreshold;
    }

    @Transactional
    public TransactionResponse deposit(String cardNumber, TransactionRequest request) {
        BigDecimal amount = validateAmount(request.amount());

        Account account = ledgerService.requireAccountByCard(cardNumber);
        ledgerService.assertActive(account);
        Account cash = ledgerService.requireSystemCashAccount();

        ledgerService.post(
                Transaction.TYPE_DEPOSIT,
                "Cash deposit",
                cardNumber,
                List.of(
                        LedgerService.Leg.debit(cash.getAccountId(), amount),
                        LedgerService.Leg.credit(account.getAccountId(), amount)));

        Account updated = ledgerService.requireAccountByCard(cardNumber);
        return new TransactionResponse(
                "Rs. " + amount.stripTrailingZeros().toPlainString() + " Deposited Successfully",
                updated.getBalance());
    }

    @Transactional
    public TransactionResponse withdraw(String cardNumber, TransactionRequest request) {
        BigDecimal amount = validateAmount(request.amount());

        if (amount.compareTo(maxWithdrawal) > 0) {
            throw new BusinessRuleException(
                    "Maximum withdrawal is Rs." + maxWithdrawal.stripTrailingZeros().toPlainString());
        }

        boolean otpRequired = amount.compareTo(BigDecimal.valueOf(otpThreshold)) >= 0;
        boolean otpProvided = request.otpId() != null && !request.otpId().isBlank()
                || request.otp() != null && !request.otp().isBlank();
        if (otpRequired || otpProvided) {
            otpService.verify(cardNumber, "WITHDRAW", request.otpId(), request.otp());
        }

        Account account = ledgerService.requireAccountByCard(cardNumber);
        ledgerService.assertActive(account);
        ledgerService.assertSufficientBalance(account, amount);
        ledgerService.assertDailyLimit(account, amount);
        Account cash = ledgerService.requireSystemCashAccount();

        ledgerService.post(
                Transaction.TYPE_WITHDRAWL,
                "Cash withdrawal",
                cardNumber,
                List.of(
                        LedgerService.Leg.debit(account.getAccountId(), amount),
                        LedgerService.Leg.credit(cash.getAccountId(), amount)));

        Account updated = ledgerService.requireAccountByCard(cardNumber);
        return new TransactionResponse(
                "Rs. " + amount.stripTrailingZeros().toPlainString() + " Withdrawn Successfully",
                updated.getBalance());
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessRuleException("Please enter the Amount");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new BusinessRuleException("Amount can have at most 2 decimal places");
        }
        return amount;
    }
}
