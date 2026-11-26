package com.example.cyber.security.signature;

import com.example.cyber.auth.domain.Customer;
import com.example.cyber.auth.domain.CustomerSigningKey;
import com.example.cyber.auth.infrastructure.CustomerRepository;
import com.example.cyber.transaction.domain.BankTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustomerTransactionSignatureService {
    public static final String ALGORITHM = "SHA256withECDSA";
    public static final String CANONICAL_VERSION = "northstar-customer-authorization-v1";
    private final CustomerRepository customers;

    public CustomerTransactionSignatureService(CustomerRepository customers) {
        this.customers = customers;
    }

    public void registerKey(String ownerEmail, String keyId, String publicKeyBase64) {
        try {
            validateP256(publicKeyBase64);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Customer signing public key is invalid; expected P-256 SPKI Base64");
        }
        Customer customer = customers.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer account not found"));
        customer.addSigningKey(keyId, publicKeyBase64);
        customers.save(customer);
    }

    public CustomerSigningKey verifyRequest(String ownerEmail, String keyId, String signatureBase64,
            String type, UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
            String currency, String operationId) {
        Customer customer = customers.findByEmail(ownerEmail)
                .orElseThrow(CustomerSignatureException::new);
        CustomerSigningKey key = customer.findSigningKey(keyId).orElseThrow(CustomerSignatureException::new);
        if (!verify(key.getPublicKeyBase64(), signatureBase64,
                canonicalBytes(keyId, ownerEmail, type, sourceAccountId, destinationAccountId,
                        amount, currency, operationId))) {
            throw new CustomerSignatureException();
        }
        return key;
    }

    public boolean verifyStored(BankTransaction transaction) {
        if (transaction.getCustomerSignature() == null || transaction.getCustomerPublicKeyBase64() == null
                || transaction.getCustomerSigningKeyId() == null) return false;
        String type = transaction.getCustomerSigningType();
        String operationId = transaction.getCustomerSigningOperationId();
        if (operationId == null || type == null) return false;
        return verify(transaction.getCustomerPublicKeyBase64(), transaction.getCustomerSignature(),
                canonicalBytes(transaction.getCustomerSigningKeyId(), transaction.getInitiatedBy(), type,
                        transaction.getCustomerSigningSourceAccountId(), transaction.getCustomerSigningDestinationAccountId(),
                        transaction.getAmount(), transaction.getCurrency(), operationId));
    }

    public static byte[] canonicalBytes(String keyId, String email, String type, UUID sourceAccountId,
            UUID destinationAccountId, BigDecimal amount, String currency, String operationId) {
        return String.join("\n", CANONICAL_VERSION, keyId, email, type, value(sourceAccountId),
                value(destinationAccountId), amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                currency, operationId).getBytes(StandardCharsets.UTF_8);
    }

    private static boolean verify(String publicKeyBase64, String signatureBase64, byte[] message) {
        try {
            ECPublicKey key = validateP256(publicKeyBase64);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(key);
            verifier.update(message);
            return verifier.verify(signatureBytes);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private static ECPublicKey validateP256(String publicKeyBase64) throws GeneralSecurityException {
        KeyFactory factory = KeyFactory.getInstance("EC");
        ECPublicKey key = (ECPublicKey) factory.generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(publicKeyBase64)));
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec expected = parameters.getParameterSpec(ECParameterSpec.class);
        ECParameterSpec actual = key.getParams();
        if (!expected.getCurve().equals(actual.getCurve())
                || !expected.getGenerator().equals(actual.getGenerator())
                || !expected.getOrder().equals(actual.getOrder())
                || expected.getCofactor() != actual.getCofactor()) {
            throw new IllegalArgumentException("Public key must use P-256");
        }
        return key;
    }

    private static String value(UUID value) { return value == null ? "" : value.toString(); }
}
