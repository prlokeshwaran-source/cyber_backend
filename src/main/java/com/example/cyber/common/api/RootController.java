package com.example.cyber.common.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "service", "Secure Banking API",
                "status", "running",
                "api", "/api/v1");
    }
}
