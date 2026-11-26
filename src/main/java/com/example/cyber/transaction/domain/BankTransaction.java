package com.example.cyber.transaction.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document("bank_transactions")
public class BankTransaction {
    @Id
    private String id;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private String initiatedBy;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private Instant createdAt;

    protected BankTransaction() {}

    public BankTransaction(UUID sourceAccountId, UUID destinationAccountId, String initiatedBy,
                           BigDecimal amount, String currency, TransactionType type) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.initiatedBy = initiatedBy;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public String getInitiatedBy() { return initiatedBy; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
