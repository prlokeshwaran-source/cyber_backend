package com.example.cyber.auth.application;

import com.example.cyber.auth.api.CustomerResponse;
import com.example.cyber.auth.api.RegisterRequest;
import com.example.cyber.auth.domain.Customer;
import com.example.cyber.auth.infrastructure.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        Customer customer = customerRepository.save(
                new Customer(normalizedEmail, passwordEncoder.encode(request.password())));
        return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getCreatedAt());
    }
}
