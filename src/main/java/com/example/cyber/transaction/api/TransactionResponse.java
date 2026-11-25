package com.example.cyber.transaction.api;

import com.example.cyber.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(Long transactionId, TransactionType type, UUID sourceAccountId,
                                 UUID destinationAccountId, BigDecimal amount, String currency,
                                 Instant createdAt) {}
