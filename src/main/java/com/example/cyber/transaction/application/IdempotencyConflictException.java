package com.example.cyber.transaction.application;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Idempotency key was already used for a different transfer");
    }
}
