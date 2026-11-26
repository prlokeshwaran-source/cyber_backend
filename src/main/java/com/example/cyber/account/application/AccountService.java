package com.example.cyber.account.application;

import com.example.cyber.account.api.AccountResponse;
import com.example.cyber.account.api.CreateAccountRequest;
import com.example.cyber.account.domain.BankAccount;
import com.example.cyber.account.infrastructure.AccountRepository;
import com.example.cyber.common.exception.ResourceNotFoundException;
import com.example.cyber.transaction.domain.BankTransaction;
import com.example.cyber.transaction.domain.TransactionType;
import com.example.cyber.transaction.infrastructure.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.cyber.security.signature.TransactionSignatureService;
import com.example.cyber.security.signature.CustomerTransactionSignatureService;
import com.example.cyber.auth.domain.CustomerSigningKey;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionSignatureService signatureService;
    private final CustomerTransactionSignatureService customerSignatureService;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                          TransactionSignatureService signatureService,
                          CustomerTransactionSignatureService customerSignatureService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.signatureService = signatureService;
        this.customerSignatureService = customerSignatureService;
    }

    @Transactional
    public AccountResponse create(String ownerEmail, CreateAccountRequest request) {
        String currency = request.currency() == null ? "INR" : request.currency();
        BigDecimal initialAmount = request.initialAmount() == null ? BigDecimal.ZERO : request.initialAmount();
        CustomerSigningKey customerKey = null;
        if (initialAmount.signum() > 0) {
            if (request.customerSigningKeyId() == null || request.customerSignature() == null
                    || request.signingNonce() == null) {
                throw new com.example.cyber.security.signature.CustomerSignatureException();
            }
            customerKey = customerSignatureService.verifyRequest(ownerEmail, request.customerSigningKeyId(),
                    request.customerSignature(), "OPENING_DEPOSIT", null, null, initialAmount,
                    currency, request.signingNonce());
        }
        BankAccount account = accountRepository.save(new BankAccount(ownerEmail, currency, initialAmount));
        if (initialAmount.signum() > 0) {
            BankTransaction openingDeposit = new BankTransaction(null, account.getPublicId(), ownerEmail,
                    initialAmount, currency, TransactionType.DEPOSIT);
            openingDeposit.attachCustomerSignature(customerKey.getKeyId(), request.customerSignature(),
                    customerKey.getPublicKeyBase64(), "OPENING_DEPOSIT", null, null, request.signingNonce());
            signatureService.sign(openingDeposit);
            transactionRepository.save(openingDeposit);
        }
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listForOwner(String ownerEmail) {
        return accountRepository.findByOwnerEmailOrderByIdAsc(ownerEmail).stream()
                .map(AccountService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getForOwner(String ownerEmail, UUID accountId) {
        BankAccount account = accountRepository.findByPublicIdAndOwnerEmail(accountId, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return toResponse(account);
    }

    private static AccountResponse toResponse(BankAccount account) {
        return new AccountResponse(account.getPublicId(), account.getCurrency(), account.getBalance());
    }
}
