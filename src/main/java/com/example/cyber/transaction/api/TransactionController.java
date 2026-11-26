package com.example.cyber.transaction.api;

import com.example.cyber.transaction.application.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import com.example.cyber.transaction.infrastructure.TransactionRepository;
import com.example.cyber.transaction.domain.BankTransaction;
import com.example.cyber.transaction.domain.TransactionType;
import com.example.cyber.transaction.api.BalanceChangeRequest;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransferService transferService;
    private final TransactionRepository transactionRepository;

    public TransactionController(TransferService transferService, TransactionRepository transactionRepository) {
        this.transferService = transferService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(Authentication authentication,
                                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @Valid @RequestBody TransferRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must contain 1 to 128 characters");
        }
        return transferService.transfer(authentication.getName(), idempotencyKey, request);
    }

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(Authentication authentication,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody BalanceChangeRequest request) {
        validateKey(key);
        return transferService.changeBalance(authentication.getName(), key, request, TransactionType.DEPOSIT);
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(Authentication authentication,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody BalanceChangeRequest request) {
        validateKey(key);
        return transferService.changeBalance(authentication.getName(), key, request, TransactionType.WITHDRAWAL);
    }

    @GetMapping
    public List<BankTransaction> history(Authentication authentication) {
        return transactionRepository.findByInitiatedByOrderByCreatedAtDesc(authentication.getName());
    }

    private static void validateKey(String key) {
        if (key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must contain 1 to 128 characters");
        }
    }
}
