package com.example.cyber.transaction.application;

import com.example.cyber.account.domain.BankAccount;
import com.example.cyber.account.infrastructure.AccountRepository;
import com.example.cyber.common.exception.ResourceNotFoundException;
import com.example.cyber.transaction.api.TransactionResponse;
import com.example.cyber.transaction.api.TransferRequest;
import com.example.cyber.transaction.domain.BankTransaction;
import com.example.cyber.transaction.domain.TransactionType;
import com.example.cyber.transaction.infrastructure.TransactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse transfer(String ownerEmail, TransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }

        BankAccount source = accountRepository.findByPublicId(request.sourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        BankAccount destination = accountRepository.findByPublicId(request.destinationAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!source.getOwnerEmail().equals(ownerEmail)) {
            throw new ResourceNotFoundException("Account not found");
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new IllegalArgumentException("Accounts must use the same currency");
        }

        source.debit(request.amount());
        destination.credit(request.amount());
        accountRepository.save(source);
        accountRepository.save(destination);
        BankTransaction transaction = transactionRepository.save(new BankTransaction(
                source.getPublicId(), destination.getPublicId(), ownerEmail,
                request.amount(), source.getCurrency(), TransactionType.TRANSFER));
        return new TransactionResponse(transaction.getId(), transaction.getType(),
                transaction.getSourceAccountId(), transaction.getDestinationAccountId(),
                transaction.getAmount(), transaction.getCurrency(), transaction.getCreatedAt());
    }
}
