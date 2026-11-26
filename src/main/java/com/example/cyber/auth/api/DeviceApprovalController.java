package com.example.cyber.auth.api;

import com.example.cyber.auth.application.DeviceApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/device-approvals")
public class DeviceApprovalController {
    private final DeviceApprovalService service;

    public DeviceApprovalController(DeviceApprovalService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MessageResponse> decide(@Valid @RequestBody DeviceApprovalRequest request) {
        if (!service.decide(request.challengeId(), request.code(), request.approve())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Approval code is invalid or expired"));
        }
        return ResponseEntity.ok(new MessageResponse(request.approve()
                ? "This device is approved" : "This device was denied"));
    }

    public record MessageResponse(String message) {}
}
