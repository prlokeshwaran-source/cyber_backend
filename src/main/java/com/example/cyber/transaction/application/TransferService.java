package com.example.cyber.transaction.application;

import com.example.cyber.account.domain.BankAccount;
import com.example.cyber.account.infrastructure.AccountRepository;
import com.example.cyber.common.exception.ResourceNotFoundException;
import com.example.cyber.transaction.api.TransactionResponse;
import com.example.cyber.transaction.api.TransferRequest;
import com.example.cyber.transaction.domain.BankTransaction;
import com.example.cyber.transaction.domain.TransactionType;
import com.example.cyber.transaction.domain.TransferIdempotencyRecord;
import com.example.cyber.transaction.infrastructure.TransactionRepository;
import com.example.cyber.transaction.infrastructure.TransferIdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferIdempotencyRepository idempotencyRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           TransferIdempotencyRepository idempotencyRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public TransactionResponse transfer(String ownerEmail, String idempotencyKey, TransferRequest request) {
        String recordId = digest(ownerEmail + "\u0000" + idempotencyKey);
        String requestHash = digest(request.sourceAccountId() + "\u0000" + request.destinationAccountId()
                + "\u0000" + request.amount().stripTrailingZeros().toPlainString());
        var previous = idempotencyRepository.findById(recordId);
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return previous.get().getResponse();
        }

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
        TransactionResponse response = new TransactionResponse(transaction.getId(), transaction.getType(),
                transaction.getSourceAccountId(), transaction.getDestinationAccountId(),
                transaction.getAmount(), transaction.getCurrency(), transaction.getCreatedAt());
        idempotencyRepository.save(new TransferIdempotencyRecord(recordId, requestHash, response));
        return response;
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
