package com.example.cyber.transaction.infrastructure;

import com.example.cyber.transaction.domain.BankTransaction;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository extends MongoRepository<BankTransaction, String> {
    List<BankTransaction> findByInitiatedByOrderByCreatedAtDesc(String initiatedBy);
}
