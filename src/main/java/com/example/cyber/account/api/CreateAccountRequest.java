package com.example.cyber.account.api;

import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(@Pattern(regexp = "USD|EUR|INR|GBP") String currency) {}
