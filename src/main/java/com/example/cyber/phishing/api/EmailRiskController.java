package com.example.cyber.phishing.api;

import com.example.cyber.phishing.application.EmailRiskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/email-checks")
public class EmailRiskController {
    private final EmailRiskService service;

    public EmailRiskController(EmailRiskService service) { this.service = service; }

    @PostMapping
    public UrlRiskResponse check(@Valid @RequestBody EmailCheckRequest request) {
        return service.analyze(request.sender(), request.subject(), request.body());
    }
}
