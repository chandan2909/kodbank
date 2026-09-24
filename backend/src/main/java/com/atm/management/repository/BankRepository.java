package com.atm.management.repository;

import com.atm.management.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {

    List<Bank> findByCardNoOrderByDateDesc(String cardNo);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN b.type = 'Deposit' THEN b.amount ELSE -b.amount END), 0)
            FROM Bank b WHERE b.cardNo = :cardNo
            """)
    BigDecimal computeBalance(@Param("cardNo") String cardNo);
}
