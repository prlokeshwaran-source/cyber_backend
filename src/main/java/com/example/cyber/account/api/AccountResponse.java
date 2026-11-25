package com.example.cyber.account.api;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID accountId, String currency, BigDecimal balance) {}
