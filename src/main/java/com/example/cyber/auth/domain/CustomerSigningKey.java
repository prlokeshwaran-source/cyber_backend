package com.example.cyber.auth.domain;

import java.time.Instant;

public class CustomerSigningKey {
    private String keyId;
    private String publicKeyBase64;
    private Instant createdAt;

    protected CustomerSigningKey() {}

    public CustomerSigningKey(String keyId, String publicKeyBase64) {
        this.keyId = keyId;
        this.publicKeyBase64 = publicKeyBase64;
        this.createdAt = Instant.now();
    }

    public String getKeyId() { return keyId; }
    public String getPublicKeyBase64() { return publicKeyBase64; }
    public Instant getCreatedAt() { return createdAt; }
}
