package com.example.cyber.auth.application;

public interface EmailNotificationSender {
    void sendDeviceApproval(String recipient, String challengeId, String code, String ipAddress, String expiresAt);
}
