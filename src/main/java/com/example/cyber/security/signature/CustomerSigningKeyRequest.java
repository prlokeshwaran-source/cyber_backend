package com.example.cyber.security.signature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSigningKeyRequest(@NotBlank @Size(max = 64) String keyId,
                                        @NotBlank @Size(max = 512) String publicKeyBase64) {}
