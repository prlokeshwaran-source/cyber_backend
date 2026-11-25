package com.example.cyber.account.infrastructure;

import com.example.cyber.account.domain.BankAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByOwnerEmailOrderByIdAsc(String ownerEmail);
    Optional<BankAccount> findByPublicIdAndOwnerEmail(UUID publicId, String ownerEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from BankAccount account where account.publicId = :publicId")
    Optional<BankAccount> findByPublicIdForUpdate(@Param("publicId") UUID publicId);
}
