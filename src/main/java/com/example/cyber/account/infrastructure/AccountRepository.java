package com.example.cyber.account.infrastructure;

import com.example.cyber.account.domain.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<BankAccount, String> {
    List<BankAccount> findByOwnerEmailOrderByIdAsc(String ownerEmail);
    Optional<BankAccount> findByPublicIdAndOwnerEmail(UUID publicId, String ownerEmail);

    Optional<BankAccount> findByPublicId(UUID publicId);
}
