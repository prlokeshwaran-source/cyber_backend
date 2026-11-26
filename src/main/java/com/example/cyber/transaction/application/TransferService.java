package com.example.cyber.transaction.application;

import com.example.cyber.account.domain.BankAccount;
import com.example.cyber.account.infrastructure.AccountRepository;
import com.example.cyber.common.exception.ResourceNotFoundException;
import com.example.cyber.transaction.api.TransactionResponse;
import com.example.cyber.transaction.api.TransferRequest;
import com.example.cyber.transaction.api.BalanceChangeRequest;
import com.example.cyber.transaction.domain.BankTransaction;
import com.example.cyber.transaction.domain.TransactionType;
import com.example.cyber.transaction.domain.TransferIdempotencyRecord;
import com.example.cyber.transaction.infrastructure.TransactionRepository;
import com.example.cyber.transaction.infrastructure.TransferIdempotencyRepository;
import com.example.cyber.security.signature.TransactionSignatureService;
import com.example.cyber.transaction.api.TransactionVerificationResponse;
import com.example.cyber.security.signature.CustomerTransactionSignatureService;
import com.example.cyber.auth.domain.CustomerSigningKey;
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
    private final TransactionSignatureService signatureService;
    private final CustomerTransactionSignatureService customerSignatureService;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           TransferIdempotencyRepository idempotencyRepository,
                           TransactionSignatureService signatureService,
                           CustomerTransactionSignatureService customerSignatureService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.signatureService = signatureService;
        this.customerSignatureService = customerSignatureService;
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

        CustomerSigningKey customerKey = customerSignatureService.verifyRequest(ownerEmail,
                request.customerSigningKeyId(), request.customerSignature(), TransactionType.TRANSFER.name(),
                source.getPublicId(), destination.getPublicId(), request.amount(), source.getCurrency(), idempotencyKey);

        source.debit(request.amount());
        destination.credit(request.amount());
        accountRepository.save(source);
        accountRepository.save(destination);
        BankTransaction transaction = new BankTransaction(
                source.getPublicId(), destination.getPublicId(), ownerEmail,
                request.amount(), source.getCurrency(), TransactionType.TRANSFER);
        transaction.attachCustomerSignature(customerKey.getKeyId(), request.customerSignature(),
                customerKey.getPublicKeyBase64(), TransactionType.TRANSFER.name(), source.getPublicId(),
                destination.getPublicId(), idempotencyKey);
        signatureService.sign(transaction);
        transaction = transactionRepository.save(transaction);
        TransactionResponse response = new TransactionResponse(transaction.getId(), transaction.getType(),
                transaction.getSourceAccountId(), transaction.getDestinationAccountId(),
                transaction.getInitiatedBy(), transaction.getAmount(), transaction.getCurrency(), transaction.getCreatedAt(),
                transaction.getSignatureVersion(), transaction.getSignatureAlgorithm(), transaction.getSigningKeyId(),
                transaction.getSignature(), transaction.getCustomerSigningKeyId(), transaction.getCustomerSignature());
        idempotencyRepository.save(new TransferIdempotencyRecord(recordId, requestHash, response));
        return response;
    }

    @Transactional
    public TransactionResponse changeBalance(String ownerEmail, String idempotencyKey,
                                             BalanceChangeRequest request, TransactionType type) {
        String recordId = digest(ownerEmail + "\u0000" + type + "\u0000" + idempotencyKey);
        String requestHash = digest(request.accountId() + "\u0000" + request.amount().stripTrailingZeros().toPlainString());
        var previous = idempotencyRepository.findById(recordId);
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(requestHash)) throw new IdempotencyConflictException();
            return previous.get().getResponse();
        }
        BankAccount account = accountRepository.findByPublicIdAndOwnerEmail(request.accountId(), ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        CustomerSigningKey customerKey = customerSignatureService.verifyRequest(ownerEmail,
                request.customerSigningKeyId(), request.customerSignature(), type.name(),
                type == TransactionType.WITHDRAWAL ? account.getPublicId() : null,
                type == TransactionType.DEPOSIT ? account.getPublicId() : null,
                request.amount(), account.getCurrency(), idempotencyKey);
        if (type == TransactionType.DEPOSIT) account.credit(request.amount());
        else account.debit(request.amount());
        accountRepository.save(account);
        BankTransaction transaction = new BankTransaction(
                type == TransactionType.DEPOSIT ? null : account.getPublicId(),
                type == TransactionType.DEPOSIT ? account.getPublicId() : null,
                ownerEmail, request.amount(), account.getCurrency(), type);
        transaction.attachCustomerSignature(customerKey.getKeyId(), request.customerSignature(),
                customerKey.getPublicKeyBase64(), type.name(),
                type == TransactionType.WITHDRAWAL ? account.getPublicId() : null,
                type == TransactionType.DEPOSIT ? account.getPublicId() : null, idempotencyKey);
        signatureService.sign(transaction);
        transaction = transactionRepository.save(transaction);
        TransactionResponse response = new TransactionResponse(transaction.getId(), transaction.getType(),
                transaction.getSourceAccountId(), transaction.getDestinationAccountId(),
                transaction.getInitiatedBy(), transaction.getAmount(), transaction.getCurrency(), transaction.getCreatedAt(),
                transaction.getSignatureVersion(), transaction.getSignatureAlgorithm(), transaction.getSigningKeyId(),
                transaction.getSignature(), transaction.getCustomerSigningKeyId(), transaction.getCustomerSignature());
        idempotencyRepository.save(new TransferIdempotencyRecord(recordId, requestHash, response));
        return response;
    }

    @Transactional(readOnly = true)
    public TransactionVerificationResponse verifyTransaction(String ownerEmail, String transactionId) {
        BankTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getInitiatedBy().equals(ownerEmail)) {
            throw new ResourceNotFoundException("Transaction not found");
        }
        boolean backendValid = signatureService.verify(transaction);
        boolean customerValid = customerSignatureService.verifyStored(transaction);
        return new TransactionVerificationResponse(transaction.getId(), backendValid && customerValid,
                backendValid, customerValid, transaction.getSignatureVersion(), transaction.getSignatureAlgorithm(),
                transaction.getSigningKeyId(), transaction.getCustomerSigningKeyId());
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
