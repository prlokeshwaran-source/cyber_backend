package com.example.cyber.config;

import com.example.cyber.auth.application.EmailNotificationSender;
import com.example.cyber.auth.infrastructure.ResendEmailNotificationSender;
import com.example.cyber.auth.infrastructure.UnavailableEmailNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EmailSenderConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "resend", name = "api-key")
    EmailNotificationSender resendEmailNotificationSender(
            @Value("${resend.api-key}") String apiKey,
            @Value("${app.security.email-from}") String from) {
        return new ResendEmailNotificationSender(RestClient.builder().baseUrl("https://api.resend.com").build(), apiKey, from);
    }

    @Bean
    @ConditionalOnMissingBean(EmailNotificationSender.class)
    EmailNotificationSender unavailableEmailNotificationSender() {
        return new UnavailableEmailNotificationSender();
    }
}
