package com.example.cyber.security.signature;

import com.example.cyber.transaction.api.TransactionSigningKeyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/transaction-signing-key")
public class TransactionSigningKeyController {
    private final TransactionSignatureService signatureService;

    public TransactionSigningKeyController(TransactionSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @GetMapping
    public TransactionSigningKeyResponse publicKey() {
        return new TransactionSigningKeyResponse(TransactionSignatureService.ALGORITHM,
                signatureService.keyId(), signatureService.publicKeyBase64());
    }
}
