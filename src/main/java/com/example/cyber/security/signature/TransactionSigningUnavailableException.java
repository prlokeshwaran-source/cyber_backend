package com.example.cyber.security.signature;

public class TransactionSigningUnavailableException extends RuntimeException {
    public TransactionSigningUnavailableException() {
        super("Transaction signing keys are not configured");
    }
}
