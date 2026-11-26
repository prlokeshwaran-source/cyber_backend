package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.EmailNotificationSender;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendEmailNotificationSender implements EmailNotificationSender {
    private static final Logger logger = LoggerFactory.getLogger(ResendEmailNotificationSender.class);
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

        try {
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
        } catch (RestClientResponseException exception) {
            String requestId = exception.getResponseHeaders() == null
                    ? null : exception.getResponseHeaders().getFirst("x-resend-id");
            logger.error("Resend rejected device approval email: status={}, requestId={}",
                    exception.getStatusCode().value(), requestId);
            throw exception;
        } catch (ResourceAccessException exception) {
            logger.error("Could not reach Resend API: {}", exception.getClass().getSimpleName());
            throw exception;
        }
    }
}
