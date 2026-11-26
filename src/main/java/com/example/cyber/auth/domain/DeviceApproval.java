package com.example.cyber.auth.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("device_approvals")
public class DeviceApproval {
    @Id private String id;
    @Indexed(unique = true) private String codeHash;
    private String email;
    private String deviceHash;
    private String requesterIp;
    private Instant expiresAt;
    private String status;
    private int failedAttempts;

    protected DeviceApproval() {}

    public DeviceApproval(String codeHash, String email, String deviceHash, String requesterIp, Instant expiresAt) {
        this.codeHash = codeHash;
        this.email = email;
        this.deviceHash = deviceHash;
        this.requesterIp = requesterIp;
        this.expiresAt = expiresAt;
        this.status = "PENDING";
    }
    public String getEmail() { return email; }
    public String getId() { return id; }
    public String getDeviceHash() { return deviceHash; }
    public String getCodeHash() { return codeHash; }
    public String getRequesterIp() { return requesterIp; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }
    public int getFailedAttempts() { return failedAttempts; }
    public void failAttempt() { failedAttempts++; if (failedAttempts >= 5) status = "DENIED"; }
    public void decide(boolean approved) { status = approved ? "APPROVED" : "DENIED"; }
}
