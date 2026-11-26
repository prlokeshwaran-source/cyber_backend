package com.example.cyber.config;

import com.example.cyber.auth.application.EmailNotificationSender;
import com.example.cyber.auth.infrastructure.SmtpEmailNotificationSender;
import com.example.cyber.auth.infrastructure.UnavailableEmailNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailSenderConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    EmailNotificationSender smtpEmailNotificationSender(JavaMailSender sender,
            @Value("${app.security.email-from:no-reply@localhost}") String from) {
        return new SmtpEmailNotificationSender(sender, from);
    }

    @Bean
    @ConditionalOnMissingBean(EmailNotificationSender.class)
    EmailNotificationSender unavailableEmailNotificationSender() {
        return new UnavailableEmailNotificationSender();
    }
}
