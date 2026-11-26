package com.example.cyber.auth.api;

import java.time.Instant;

public record CustomerResponse(String id, String email, Instant createdAt) {}
