package com.example.cyber.transaction.api;

import com.example.cyber.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(String transactionId, TransactionType type, UUID sourceAccountId,
                                 UUID destinationAccountId, String initiatedBy, BigDecimal amount, String currency,
                                 Instant createdAt, String signatureVersion, String signatureAlgorithm, String signingKeyId,
                                 String signature, String customerSigningKeyId, String customerSignature) {}
