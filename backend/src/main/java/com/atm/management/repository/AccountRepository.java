package com.atm.management.repository;

import com.atm.management.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNo(String accountNo);

    Optional<Account> findByFormNo(String formNo);

    Optional<Account> findByAccountNoIgnoreCase(String accountNo);

    List<Account> findAllByOrderByAccountIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNo = :accountNo")
    Optional<Account> findByAccountNoForUpdate(@Param("accountNo") String accountNo);

    boolean existsByAccountNo(String accountNo);

    @Query("SELECT count(a) > 0 FROM Account a WHERE a.system = 1")
    boolean existsSystemAccount();
}
