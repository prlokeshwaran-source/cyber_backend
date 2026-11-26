package com.example.cyber.phishing.api;

import com.example.cyber.phishing.application.UrlRiskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/url-checks")
public class UrlRiskController {
    private final UrlRiskService service;

    public UrlRiskController(UrlRiskService service) { this.service = service; }

    @PostMapping
    public UrlRiskResponse check(@Valid @RequestBody UrlCheckRequest request) {
        return service.analyze(request.url());
    }
}
