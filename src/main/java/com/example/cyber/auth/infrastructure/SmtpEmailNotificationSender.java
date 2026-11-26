package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.EmailNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpEmailNotificationSender implements EmailNotificationSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailNotificationSender(JavaMailSender mailSender,
            @Value("${app.security.email-from:no-reply@localhost}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendDeviceApproval(String recipient, String challengeId, String code, String ipAddress, String expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Approve a new device for your banking account");
        message.setText("A device at IP address " + ipAddress + " tried to access your banking account.\n\n"
                + "If this was you, enter challenge " + challengeId + " and this one-time approval code in the app: " + code + "\n"
                + "If this was not you, do not share the code and choose Deny in the app.\n"
                + "The code expires at " + expiresAt + ". Approval trusts this device for future access.\n"
                + "If you did not request this, change your password and contact support.");
        mailSender.send(message);
    }
}
