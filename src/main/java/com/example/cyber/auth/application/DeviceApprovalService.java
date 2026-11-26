package com.example.cyber.auth.application;

import com.example.cyber.auth.domain.DeviceApproval;
import com.example.cyber.auth.infrastructure.CustomerRepository;
import com.example.cyber.auth.infrastructure.DeviceApprovalRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceApprovalService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final CustomerRepository customers;
    private final DeviceApprovalRepository approvals;
    private final EmailNotificationSender emailSender;

    public DeviceApprovalService(CustomerRepository customers, DeviceApprovalRepository approvals,
                                 EmailNotificationSender emailSender) {
        this.customers = customers;
        this.approvals = approvals;
        this.emailSender = emailSender;
    }

    public boolean trusted(String email, String deviceId) {
        String deviceHash = digest(deviceId);
        return customers.findByEmail(email).map(customer -> customer.trustsDevice(deviceHash)).orElse(false);
    }

    public void requestApproval(String email, String deviceId, String ip) {
        String deviceHash = digest(deviceId);
        var previous = approvals.findFirstByEmailAndDeviceHashOrderByExpiresAtDesc(email, deviceHash);
        Instant now = Instant.now();
        if (previous.isPresent() && previous.get().getExpiresAt().isAfter(now)
                && "DENIED".equals(previous.get().getStatus())) return;
        if (previous.isPresent() && previous.get().getExpiresAt().isAfter(now)
                && "PENDING".equals(previous.get().getStatus())
                && previous.get().getLastSentAt() != null
                && previous.get().getLastSentAt().plusSeconds(60).isAfter(now)) return;

        String code = String.format("%08d", RANDOM.nextInt(100_000_000));
        Instant expiry = now.plusSeconds(600);
        DeviceApproval approval = previous.isPresent() && "PENDING".equals(previous.get().getStatus())
                ? previous.get() : new DeviceApproval(digest(code), email, deviceHash, ip, expiry);
        if (previous.isPresent() && approval == previous.get()) approval.refreshCode(digest(code), expiry);
        approval = approvals.save(approval);
        try {
            emailSender.sendDeviceApproval(email, approval.getId(), code, ip, expiry.toString());
        } catch (RuntimeException exception) {
            approvals.delete(approval);
            throw new DeviceApprovalDeliveryException(exception);
        }
    }

    @Transactional
    public boolean decide(String challengeId, String code, boolean approve) {
        DeviceApproval approval = approvals.findById(challengeId).orElse(null);
        if (approval == null || !"PENDING".equals(approval.getStatus())
                || !approval.getExpiresAt().isAfter(Instant.now())) return false;
        if (!MessageDigest.isEqual(approval.getCodeHash().getBytes(StandardCharsets.UTF_8),
                digest(code).getBytes(StandardCharsets.UTF_8))) {
            approval.failAttempt();
            approvals.save(approval);
            return false;
        }
        if (!approve) {
            approval.decide(false);
            approvals.save(approval);
            return true;
        }
        var customer = customers.findByEmail(approval.getEmail())
                .orElse(null);
        if (customer == null) return false;
        customer.trustDevice(approval.getDeviceHash());
        approval.decide(true);
        customers.save(customer);
        approvals.save(approval);
        return true;
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
