package com.atm.management.service;

import com.atm.management.dto.request.BeneficiaryRequest;
import com.atm.management.dto.response.BeneficiaryResponse;
import com.atm.management.entity.Account;
import com.atm.management.entity.Beneficiary;
import com.atm.management.entity.Signup;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.exception.ConflictException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.AccountRepository;
import com.atm.management.repository.BeneficiaryRepository;
import com.atm.management.repository.LoginRepository;
import com.atm.management.repository.SignupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final LoginRepository loginRepository;
    private final SignupRepository signupRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            AccountRepository accountRepository,
            LoginRepository loginRepository,
            SignupRepository signupRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.loginRepository = loginRepository;
        this.signupRepository = signupRepository;
    }

    private Long ownerAccountId(String cardNo) {
        return loginRepository.findByCardNo(cardNo)
                .flatMap(login -> login.getAccountId() == null
                        ? java.util.Optional.empty()
                        : java.util.Optional.of(login.getAccountId()))
                .orElseThrow(() -> new NotFoundException("No bank account is linked to this card."));
    }

    public List<BeneficiaryResponse> list(String cardNo) {
        Long ownerId = ownerAccountId(cardNo);
        return beneficiaryRepository.findByOwnerAccountIdOrderByCreatedAtDesc(ownerId).stream()
                .map(b -> new BeneficiaryResponse(
                        b.getId(),
                        b.getBeneficiaryAccountNo(),
                        b.getBeneficiaryName(),
                        b.getNickname(),
                        b.getStatus(),
                        b.getCreatedAt()))
                .toList();
    }

    @Transactional
    public BeneficiaryResponse add(String cardNo, BeneficiaryRequest request) {
        Long ownerId = ownerAccountId(cardNo);
        String accountNo = request.accountNo().trim();

        Account beneficiary = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account number not found."));

        if (beneficiary.getAccountId().equals(ownerId)) {
            throw new BusinessRuleException("Cannot add your own account as a beneficiary");
        }
        if (beneficiary.isSystem()) {
            throw new BusinessRuleException("Invalid account number");
        }

        if (beneficiaryRepository
                .findByOwnerAccountIdAndBeneficiaryAccountNo(ownerId, accountNo).isPresent()) {
            throw new ConflictException("Beneficiary already exists");
        }

        Beneficiary b = new Beneficiary();
        b.setOwnerAccountId(ownerId);
        b.setBeneficiaryAccountNo(accountNo);
        b.setNickname(request.nickname() != null && !request.nickname().isBlank()
                ? request.nickname().trim() : null);
        b.setBeneficiaryName(lookupHolderName(beneficiary));
        b.setStatus("ACTIVE");
        b.setCreatedAt(LocalDateTime.now());
        beneficiaryRepository.save(b);

        return new BeneficiaryResponse(
                b.getId(), b.getBeneficiaryAccountNo(), b.getBeneficiaryName(),
                b.getNickname(), b.getStatus(), b.getCreatedAt());
    }

    @Transactional
    public void remove(String cardNo, Long beneficiaryId) {
        Long ownerId = ownerAccountId(cardNo);
        Beneficiary b = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new NotFoundException("Beneficiary not found."));
        if (!b.getOwnerAccountId().equals(ownerId)) {
            throw new NotFoundException("Beneficiary not found.");
        }
        beneficiaryRepository.delete(b);
    }

    private String lookupHolderName(Account account) {
        if (account.getFormNo() == null) {
            return null;
        }
        return signupRepository.findById(account.getFormNo())
                .map(Signup::getName)
                .orElse(null);
    }
}
