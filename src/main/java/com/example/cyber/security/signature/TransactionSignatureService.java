package com.example.cyber.security.signature;

import com.example.cyber.transaction.domain.BankTransaction;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.AlgorithmParameters;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Signs the canonical transaction fields using ECDSA over the NIST P-256 curve and SHA-256. */
@Service
public class TransactionSignatureService {
    public static final String ALGORITHM = "SHA256withECDSA";
    public static final String CANONICAL_VERSION = "northstar-transaction-v2";
    private static final String PREVIOUS_CANONICAL_VERSION = "northstar-transaction-v1";
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keyId;

    public TransactionSignatureService(
            @Value("${app.signing.private-key-base64:}") String privateKeyBase64,
            @Value("${app.signing.public-key-base64:}") String publicKeyBase64,
            @Value("${app.signing.key-id:northstar-p256-v1}") String keyId) {
        this.keyId = keyId;
        if (privateKeyBase64.isBlank() && publicKeyBase64.isBlank()) {
            this.privateKey = null;
            this.publicKey = null;
            return;
        }
        if (privateKeyBase64.isBlank() || publicKeyBase64.isBlank()) {
            throw new IllegalStateException("Both transaction signing keys must be configured together");
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(privateKeyBase64.trim())));
            this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(
                    Base64.getDecoder().decode(publicKeyBase64.trim())));
            verifyKeyPair();
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Transaction signing keys are invalid", exception);
        }
    }

    public void sign(BankTransaction transaction) {
        requireConfigured();
        try {
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(privateKey);
            signer.update(canonicalBytes(transaction, CANONICAL_VERSION));
            transaction.attachSignature(CANONICAL_VERSION, ALGORITHM, keyId,
                    Base64.getEncoder().encodeToString(signer.sign()));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign transaction", exception);
        }
    }

    public boolean verify(BankTransaction transaction) {
        requireConfigured();
        if (!ALGORITHM.equals(transaction.getSignatureAlgorithm())
                || !(CANONICAL_VERSION.equals(transaction.getSignatureVersion())
                || PREVIOUS_CANONICAL_VERSION.equals(transaction.getSignatureVersion()))
                || !keyId.equals(transaction.getSigningKeyId())
                || transaction.getSignature() == null) return false;
        try {
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(canonicalBytes(transaction, transaction.getSignatureVersion()));
            return verifier.verify(Base64.getDecoder().decode(transaction.getSignature()));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String publicKeyBase64() {
        requireConfigured();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String keyId() { return keyId; }

    private void requireConfigured() {
        if (privateKey == null || publicKey == null) throw new TransactionSigningUnavailableException();
    }

    private void verifyKeyPair() throws GeneralSecurityException {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec expected = parameters.getParameterSpec(ECParameterSpec.class);
        if (!(privateKey instanceof ECPrivateKey ecPrivate)
                || !(publicKey instanceof ECPublicKey ecPublic)
                || !sameCurve(expected, ecPrivate.getParams())
                || !sameCurve(expected, ecPublic.getParams())) {
            throw new IllegalArgumentException("Signing keys must use the P-256 curve");
        }
        byte[] challenge = "northstar-signing-key-pair-check".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance(ALGORITHM);
        signer.initSign(privateKey);
        signer.update(challenge);
        byte[] signed = signer.sign();
        Signature verifier = Signature.getInstance(ALGORITHM);
        verifier.initVerify(publicKey);
        verifier.update(challenge);
        if (!verifier.verify(signed)) throw new IllegalArgumentException("Public and private keys do not match");
    }

    private static boolean sameCurve(ECParameterSpec expected, ECParameterSpec actual) {
        return expected.getCurve().equals(actual.getCurve())
                && expected.getGenerator().equals(actual.getGenerator())
                && expected.getOrder().equals(actual.getOrder())
                && expected.getCofactor() == actual.getCofactor();
    }

    /** Newline-delimited UTF-8 fields in this fixed order; nullable account IDs are empty. */
    private static byte[] canonicalBytes(BankTransaction transaction, String version) {
        String canonical = String.join("\n",
                version,
                transaction.getId(),
                transaction.getType().name(),
                value(transaction.getSourceAccountId()),
                value(transaction.getDestinationAccountId()),
                transaction.getInitiatedBy(),
                transaction.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                transaction.getCurrency(),
                transaction.getCreatedAt().truncatedTo(ChronoUnit.MILLIS).toString());
        if (CANONICAL_VERSION.equals(version)) {
            canonical += "\n" + value(transaction.getCustomerSigningKeyId())
                    + "\n" + value(transaction.getCustomerPublicKeyBase64())
                    + "\n" + value(transaction.getCustomerSignature())
                    + "\n" + value(transaction.getCustomerSigningType())
                    + "\n" + value(transaction.getCustomerSigningSourceAccountId())
                    + "\n" + value(transaction.getCustomerSigningDestinationAccountId())
                    + "\n" + value(transaction.getCustomerSigningOperationId());
        }
        return canonical.getBytes(StandardCharsets.UTF_8);
    }

    private static String value(UUID value) { return value == null ? "" : value.toString(); }
    private static String value(String value) { return value == null ? "" : value; }
}
