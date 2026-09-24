package com.atm.management.service;

import com.atm.management.entity.Transaction;
import com.atm.management.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionLookupService {

    private final TransactionRepository transactionRepository;

    public TransactionLookupService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Map<Long, String> typesFor(Collection<Long> txnIds) {
        Map<Long, String> result = new HashMap<>();
        if (txnIds == null || txnIds.isEmpty()) {
            return result;
        }
        Set<Long> unique = txnIds.stream().collect(Collectors.toSet());
        List<Transaction> txns = transactionRepository.findAllById(unique);
        for (Transaction t : txns) {
            result.put(t.getId(), t.getType());
        }
        return result;
    }
}
