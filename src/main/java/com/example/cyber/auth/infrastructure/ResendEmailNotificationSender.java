package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.EmailNotificationSender;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class ResendEmailNotificationSender implements EmailNotificationSender {
    private final RestClient restClient;
    private final String apiKey;
    private final String from;

    public ResendEmailNotificationSender(RestClient restClient, String apiKey, String from) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void sendDeviceApproval(String recipient, String challengeId, String code, String ipAddress, String expiresAt) {
        String body = "A device at IP address " + ipAddress + " tried to access your banking account.\n\n"
                + "If this was you, enter challenge " + challengeId + " and this one-time approval code in the app: " + code + "\n"
                + "If this was not you, do not share the code and choose Deny in the app.\n"
                + "The code expires at " + expiresAt + ". Approval trusts this device for future access.\n"
                + "If you did not request this, change your password and contact support.";

        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                        "from", from,
                        "to", List.of(recipient),
                        "subject", "Approve a new device for your banking account",
                        "text", body))
                .retrieve()
                .toBodilessEntity();
    }
}
