package com.example.cyber.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record BalanceChangeRequest(
        @NotNull UUID accountId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 64) String customerSigningKeyId,
        @NotBlank @Size(max = 256) String customerSignature) {}
