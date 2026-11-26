package com.example.cyber.phishing.api;

import java.util.List;

public record UrlRiskResponse(String riskLevel, int riskScore, List<String> reasons) {}
