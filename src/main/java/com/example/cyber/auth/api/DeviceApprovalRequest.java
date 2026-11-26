package com.example.cyber.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceApprovalRequest(@NotBlank @Size(max = 64) String challengeId,
                                    @NotBlank @Pattern(regexp = "\\d{8}") String code,
                                    boolean approve) {}
