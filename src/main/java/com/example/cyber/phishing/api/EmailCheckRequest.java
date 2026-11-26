package com.example.cyber.phishing.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailCheckRequest(
        @Email @Size(max = 254) String sender,
        @Size(max = 300) String subject,
        @NotBlank @Size(max = 20000) String body) {}
