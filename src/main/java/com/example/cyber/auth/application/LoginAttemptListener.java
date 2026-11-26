package com.example.cyber.auth.application;

import com.example.cyber.auth.infrastructure.CustomerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptListener {
    private final CustomerRepository repository;

    public LoginAttemptListener(CustomerRepository repository) { this.repository = repository; }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName().trim().toLowerCase();
        repository.findByEmail(email).ifPresent(customer -> {
            customer.recordFailedLogin();
            repository.save(customer);
        });
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        repository.findByEmail(event.getAuthentication().getName()).ifPresent(customer -> {
            customer.recordSuccessfulLogin();
            repository.save(customer);
        });
    }
}
