package com.example.cyber.auth.application;

public class DeviceApprovalDeliveryException extends RuntimeException {
    public DeviceApprovalDeliveryException(Throwable cause) {
        super("Unable to send device approval email", cause);
    }
}
