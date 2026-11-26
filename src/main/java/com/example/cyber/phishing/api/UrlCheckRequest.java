package com.example.cyber.phishing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UrlCheckRequest(@NotBlank @Size(max = 2048) String url) {}
