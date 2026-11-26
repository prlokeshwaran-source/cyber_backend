package com.example.cyber.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private List<String> trustedDeviceHashes = new ArrayList<>();

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
    public boolean trustsDevice(String deviceHash) { return trustedDeviceHashes.contains(deviceHash); }
    public void trustDevice(String deviceHash) {
        if (!trustedDeviceHashes.contains(deviceHash)) trustedDeviceHashes.add(deviceHash);
    }
}
