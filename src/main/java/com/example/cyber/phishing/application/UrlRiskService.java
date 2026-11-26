package com.example.cyber.phishing.application;

import com.example.cyber.phishing.api.UrlRiskResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class UrlRiskService {
    public UrlRiskResponse analyze(String raw) {
        List<String> reasons = new ArrayList<>();
        int score = 0;
        URI uri;
        try {
            uri = URI.create(raw.trim());
            if (uri.getScheme() == null || uri.getHost() == null
                    || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
                return new UrlRiskResponse("HIGH", 100, List.of("Invalid or unsupported URL format"));
            }
        } catch (IllegalArgumentException exception) {
            return new UrlRiskResponse("HIGH", 100, List.of("Invalid URL format"));
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!uri.getScheme().equalsIgnoreCase("https")) { score += 20; reasons.add("URL does not use HTTPS"); }
        if (uri.getRawUserInfo() != null) { score += 40; reasons.add("URL contains embedded credentials"); }
        if (host.startsWith("xn--") || host.contains(".xn--")) { score += 35; reasons.add("Internationalized hostname may imitate another domain"); }
        if (host.matches("[0-9.]+") || host.contains(":")) { score += 35; reasons.add("URL uses an IP address instead of a domain name"); }
        if (host.split("\\.").length > 4) { score += 15; reasons.add("Hostname has an unusually deep subdomain chain"); }
        String suspicious = "login|verify|secure|account|wallet|password|update|confirm";
        if (host.matches(".*(" + suspicious + ").*")) { score += 20; reasons.add("Hostname contains credential or account bait terms"); }
        score = Math.min(100, score);
        String level = score >= 60 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";
        return new UrlRiskResponse(level, score, List.copyOf(reasons));
    }
}
