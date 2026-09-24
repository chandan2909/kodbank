package com.atm.management.repository;

import com.atm.management.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByOwnerAccountIdOrderByCreatedAtDesc(Long ownerAccountId);

    Optional<Beneficiary> findByOwnerAccountIdAndBeneficiaryAccountNo(
            Long ownerAccountId, String beneficiaryAccountNo);

    void deleteByOwnerAccountIdAndBeneficiaryAccountNo(
            Long ownerAccountId, String beneficiaryAccountNo);
}
