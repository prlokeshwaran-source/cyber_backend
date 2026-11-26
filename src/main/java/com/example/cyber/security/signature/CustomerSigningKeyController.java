package com.example.cyber.security.signature;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/customer-signing-keys")
public class CustomerSigningKeyController {
    private final CustomerTransactionSignatureService signatureService;

    public CustomerSigningKeyController(CustomerTransactionSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @PostMapping
    public CustomerSigningKeyResponse register(Authentication authentication,
                                               @Valid @RequestBody CustomerSigningKeyRequest request) {
        signatureService.registerKey(authentication.getName(), request.keyId(), request.publicKeyBase64());
        return new CustomerSigningKeyResponse(request.keyId(), true);
    }
}
