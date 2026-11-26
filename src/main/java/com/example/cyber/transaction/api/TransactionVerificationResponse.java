package com.example.cyber.transaction.api;

public record TransactionVerificationResponse(String transactionId, boolean valid,
                                              String signatureAlgorithm, String signingKeyId) {}
