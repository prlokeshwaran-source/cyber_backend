package com.example.cyber.phishing.application;

import com.example.cyber.phishing.api.UrlRiskResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Rule-based phishing/spam triage. It does not use a live reputation feed or replace mail filtering. */
@Service
public class EmailRiskService {
    private static final Pattern LINK = Pattern.compile("(?i)\\bhttps?://[^\\s<>\\\"']+");
    private final UrlRiskService urlRiskService;

    public EmailRiskService(UrlRiskService urlRiskService) { this.urlRiskService = urlRiskService; }

    public UrlRiskResponse analyze(String sender, String subject, String body) {
        String content = ((subject == null ? "" : subject) + "\n" + body).toLowerCase(Locale.ROOT);
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (containsAny(content, "verify your account", "account will be suspended", "account suspended",
                "unusual activity", "urgent action", "immediately", "within 24 hours", "limited time")) {
            score += 25; reasons.add("Urgent or threatening language commonly used in phishing messages");
        }
        if (containsAny(content, "password", "one-time code", "verification code", "otp", "security code")
                && containsAny(content, "reply", "send", "provide", "confirm", "click", "enter")) {
            score += 35; reasons.add("Message appears to request a password or verification code");
        }
        if (containsAny(content, "gift card", "wire transfer", "crypto wallet", "cryptocurrency", "claim your prize", "you have won")) {
            score += 30; reasons.add("Message contains common payment or prize scam language");
        }
        if (sender != null) {
            String domain = sender.substring(sender.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
            if (containsAny(domain, "bank", "secure", "support", "account", "verify", "official")) {
                score += 15; reasons.add("Sender domain uses trust or account terms; verify it independently");
            }
        }

        Matcher matcher = LINK.matcher(body);
        int checkedLinks = 0;
        while (matcher.find() && checkedLinks < 20) {
            String link = matcher.group().replaceAll("[),.;!?]+$", "");
            UrlRiskResponse linkResult = urlRiskService.analyze(link);
            if (linkResult.riskScore() >= 60) {
                score += 35;
                reasons.add("An embedded link has HIGH phishing risk");
            } else if (linkResult.riskScore() >= 25) {
                score += 20;
                reasons.add("An embedded link has MEDIUM phishing risk");
            }
            checkedLinks++;
        }
        if (checkedLinks == 0 && containsAny(content, "click here", "open the link", "sign in here")) {
            score += 10; reasons.add("Message urges you to follow a link, but no URL was available to inspect");
        }
        score = Math.min(score, 100);
        String level = score >= 60 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";
        if (reasons.isEmpty()) reasons.add("No configured phishing or spam indicators were detected");
        return new UrlRiskResponse(level, score, List.copyOf(reasons));
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }
}
