package com.atm.management.repository;

import com.atm.management.entity.TxnEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TxnEntryRepository extends JpaRepository<TxnEntry, Long> {

    @Query("""
            SELECT e FROM TxnEntry e, com.atm.management.entity.Transaction t
            WHERE t.id = e.txnId
              AND e.accountId = :accountId
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
              AND (:type IS NULL OR t.type = :type)
            ORDER BY t.createdAt DESC, e.id DESC
            """)
    Page<TxnEntry> search(
            @Param("accountId") Long accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") String type,
            Pageable pageable);

    @Query("""
            SELECT e FROM TxnEntry e, com.atm.management.entity.Transaction t
            WHERE t.id = e.txnId
              AND e.accountId = :accountId
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
              AND (:type IS NULL OR t.type = :type)
            ORDER BY t.createdAt DESC, e.id DESC
            """)
    List<TxnEntry> findAllForStatement(
            @Param("accountId") Long accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") String type);

    @Query("""
            SELECT e FROM TxnEntry e, com.atm.management.entity.Transaction t
            WHERE t.id = e.txnId
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
              AND (:type IS NULL OR t.type = :type)
            ORDER BY t.createdAt DESC, e.id DESC
            """)
    Page<TxnEntry> searchAll(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") String type,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM TxnEntry e
            WHERE e.accountId = :accountId
              AND e.direction = 'D'
              AND e.createdAt >= :dayStart
            """)
    BigDecimal sumDebitsSince(
            @Param("accountId") Long accountId,
            @Param("dayStart") LocalDateTime dayStart);

    List<TxnEntry> findByTxnIdOrderByIdAsc(Long txnId);
}
