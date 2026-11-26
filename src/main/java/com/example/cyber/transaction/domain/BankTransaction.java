package com.example.cyber.transaction.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

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
    private String signatureVersion;
    private String signatureAlgorithm;
    private String signingKeyId;
    private String signature;
    private String customerSigningKeyId;
    private String customerSignature;
    private String customerPublicKeyBase64;
    private String customerSigningType;
    private UUID customerSigningSourceAccountId;
    private UUID customerSigningDestinationAccountId;
    private String customerSigningOperationId;

    protected BankTransaction() {}

    public BankTransaction(UUID sourceAccountId, UUID destinationAccountId, String initiatedBy,
                           BigDecimal amount, String currency, TransactionType type) {
        this.id = UUID.randomUUID().toString();
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.initiatedBy = initiatedBy;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public String getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public String getInitiatedBy() { return initiatedBy; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSignatureVersion() { return signatureVersion; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public String getSigningKeyId() { return signingKeyId; }
    public String getSignature() { return signature; }
    public String getCustomerSigningKeyId() { return customerSigningKeyId; }
    public String getCustomerSignature() { return customerSignature; }
    public String getCustomerPublicKeyBase64() { return customerPublicKeyBase64; }
    public String getCustomerSigningType() { return customerSigningType; }
    public UUID getCustomerSigningSourceAccountId() { return customerSigningSourceAccountId; }
    public UUID getCustomerSigningDestinationAccountId() { return customerSigningDestinationAccountId; }
    public String getCustomerSigningOperationId() { return customerSigningOperationId; }

    public void attachCustomerSignature(String keyId, String signature, String publicKeyBase64,
            String signedType, UUID signedSourceAccountId, UUID signedDestinationAccountId, String operationId) {
        this.customerSigningKeyId = keyId;
        this.customerSignature = signature;
        this.customerPublicKeyBase64 = publicKeyBase64;
        this.customerSigningType = signedType;
        this.customerSigningSourceAccountId = signedSourceAccountId;
        this.customerSigningDestinationAccountId = signedDestinationAccountId;
        this.customerSigningOperationId = operationId;
    }

    public void attachSignature(String version, String algorithm, String keyId, String signature) {
        this.signatureVersion = version;
        this.signatureAlgorithm = algorithm;
        this.signingKeyId = keyId;
        this.signature = signature;
    }
}
