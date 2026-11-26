package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.EmailNotificationSender;

public class UnavailableEmailNotificationSender implements EmailNotificationSender {
    @Override
    public void sendDeviceApproval(String recipient, String challengeId, String code, String ipAddress, String expiresAt) {
        throw new IllegalStateException("SMTP is not configured; device approval email cannot be sent");
    }
}
