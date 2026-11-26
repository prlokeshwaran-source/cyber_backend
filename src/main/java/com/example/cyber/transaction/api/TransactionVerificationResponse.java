package com.example.cyber.transaction.api;

public record TransactionVerificationResponse(String transactionId, boolean valid,
                                              boolean backendSignatureValid, boolean customerSignatureValid,
                                              String signatureVersion, String signatureAlgorithm, String signingKeyId,
                                              String customerSigningKeyId) {}
