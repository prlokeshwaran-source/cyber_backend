package com.example.cyber.transaction.infrastructure;

import com.example.cyber.transaction.domain.BankTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findByInitiatedByOrderByCreatedAtDesc(String initiatedBy);
}
