package com.example.cyber.account.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @Pattern(regexp = "USD|EUR|INR|GBP") String currency,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal initialAmount,
        @Size(max = 64) String customerSigningKeyId,
        @Size(max = 256) String customerSignature,
        @Size(max = 64) String signingNonce) {}
