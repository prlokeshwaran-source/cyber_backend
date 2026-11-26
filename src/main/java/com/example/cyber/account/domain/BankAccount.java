package com.example.cyber.account.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.UUID;

@Document("bank_accounts")
public class BankAccount {
    @Id
    private String id;
    @org.springframework.data.annotation.Version
    private Long version;
    @Indexed(unique = true)
    private UUID publicId;
    private String ownerEmail;
    private BigDecimal balance;
    private String currency;

    protected BankAccount() {}

    public BankAccount(String ownerEmail, String currency) {
        this.publicId = UUID.randomUUID();
        this.ownerEmail = ownerEmail;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    public String getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getOwnerEmail() { return ownerEmail; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new com.example.cyber.common.exception.InsufficientFundsException();
        }
        balance = balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.scale() > 2 || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive with at most two decimal places");
        }
    }
}
