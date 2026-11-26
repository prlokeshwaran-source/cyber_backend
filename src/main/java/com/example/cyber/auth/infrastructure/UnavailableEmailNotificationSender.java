package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.EmailNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnavailableEmailNotificationSender implements EmailNotificationSender {
    private static final Logger logger = LoggerFactory.getLogger(UnavailableEmailNotificationSender.class);

    @Override
    public void sendDeviceApproval(String recipient, String challengeId, String code, String ipAddress, String expiresAt) {
        logger.error("Device approval email requested but RESEND_API_KEY is not configured");
        throw new IllegalStateException("Resend is not configured; device approval email cannot be sent");
    }
}
