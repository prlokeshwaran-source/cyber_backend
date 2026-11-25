package com.example.cyber.auth.api;

import java.time.Instant;

public record CustomerResponse(Long id, String email, Instant createdAt) {}
