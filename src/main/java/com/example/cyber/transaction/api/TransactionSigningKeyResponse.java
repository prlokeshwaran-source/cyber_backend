package com.example.cyber.transaction.api;

public record TransactionSigningKeyResponse(String algorithm, String keyId, String publicKeyBase64) {}
