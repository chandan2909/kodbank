package com.atm.management.service;

import com.atm.management.entity.Account;
import com.atm.management.entity.Transaction;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.dto.request.TransferRequest;
import com.atm.management.dto.response.TransactionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

    private final LedgerService ledgerService;
    private final OtpService otpService;

    public TransferService(LedgerService ledgerService, OtpService otpService) {
        this.ledgerService = ledgerService;
        this.otpService = otpService;
    }

    @Transactional
    public TransactionResponse transfer(String fromCardNo, TransferRequest request) {
        BigDecimal amount = request.amount();
        if (amount == null) {
            throw new BusinessRuleException("Please enter the Amount");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new BusinessRuleException("Amount can have at most 2 decimal places");
        }

        String toAccountNo = request.accountNo() != null && !request.accountNo().isBlank()
                ? request.accountNo().trim()
                : null;
        if (toAccountNo == null) {
            throw new BusinessRuleException("Beneficiary account number is required");
        }

        otpService.verify(fromCardNo, OtpService.PURPOSE_TRANSFER, request.otpId(), request.otp());

        Account from = ledgerService.requireAccountByCard(fromCardNo);
        ledgerService.assertActive(from);
        ledgerService.assertSufficientBalance(from, amount);
        ledgerService.assertDailyLimit(from, amount);

        Account to = ledgerService.requireAccountByNo(toAccountNo);
        if (to.getAccountId().equals(from.getAccountId())) {
            throw new BusinessRuleException("Cannot transfer to the same account");
        }
        if (to.isSystem()) {
            throw new BusinessRuleException("Invalid beneficiary account");
        }
        ledgerService.assertActive(to);

        BigDecimal newBalance = ledgerService.post(
                Transaction.TYPE_TRANSFER,
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : "Fund transfer to " + toAccountNo,
                fromCardNo,
                List.of(
                        LedgerService.Leg.debit(from.getAccountId(), amount),
                        LedgerService.Leg.credit(to.getAccountId(), amount)));

        return new TransactionResponse(
                "Rs. " + amount.stripTrailingZeros().toPlainString()
                        + " Transferred Successfully to " + toAccountNo,
                newBalance);
    }
}
