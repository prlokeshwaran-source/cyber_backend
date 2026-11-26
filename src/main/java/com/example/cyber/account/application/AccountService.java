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

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse create(String ownerEmail, CreateAccountRequest request) {
        String currency = request.currency() == null ? "INR" : request.currency();
        BigDecimal initialAmount = request.initialAmount() == null ? BigDecimal.ZERO : request.initialAmount();
        BankAccount account = accountRepository.save(new BankAccount(ownerEmail, currency, initialAmount));
        if (initialAmount.signum() > 0) {
            transactionRepository.save(new BankTransaction(null, account.getPublicId(), ownerEmail,
                    initialAmount, currency, TransactionType.DEPOSIT));
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
