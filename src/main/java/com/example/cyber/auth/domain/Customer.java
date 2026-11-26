package com.example.cyber.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Document("customers")
public class Customer {
    @Id
    private String id;
    @Indexed(unique = true)
    private String email;
    private String passwordHash;
    private Instant createdAt;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private List<CustomerSigningKey> transactionSigningKeys = new ArrayList<>();

    protected Customer() {}

    public Customer(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isLocked() { return lockedUntil != null && lockedUntil.isAfter(Instant.now()); }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void recordFailedLogin() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= 5) lockedUntil = Instant.now().plusSeconds(900);
    }
    public void recordSuccessfulLogin() { failedLoginAttempts = 0; lockedUntil = null; }
    public Optional<CustomerSigningKey> findSigningKey(String keyId) {
        return transactionSigningKeys.stream().filter(key -> key.getKeyId().equals(keyId)).findFirst();
    }
    public void addSigningKey(String keyId, String publicKeyBase64) {
        Optional<CustomerSigningKey> existing = findSigningKey(keyId);
        if (existing.isPresent()) {
            if (!existing.get().getPublicKeyBase64().equals(publicKeyBase64)) {
                throw new IllegalArgumentException("Signing key ID is already registered with different key material");
            }
            return;
        }
        if (transactionSigningKeys.size() >= 10) {
            throw new IllegalArgumentException("Maximum of 10 customer signing keys reached");
        }
        transactionSigningKeys.add(new CustomerSigningKey(keyId, publicKeyBase64));
    }
}
