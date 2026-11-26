package com.example.cyber.account.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @Pattern(regexp = "USD|EUR|INR|GBP") String currency,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal initialAmount) {}
