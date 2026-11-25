package com.example.cyber.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;
    @Column(nullable = false, updatable = false)
    private String ownerEmail;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    @Column(nullable = false, length = 3)
    private String currency;
    @Version
    private long version;

    protected BankAccount() {}

    public BankAccount(String ownerEmail, String currency) {
        this.publicId = UUID.randomUUID();
        this.ownerEmail = ownerEmail;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    public Long getId() { return id; }
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
