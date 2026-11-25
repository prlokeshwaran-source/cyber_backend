# Backend Architecture

## Purpose and status

This document defines the structure for the secure banking backend. The current code implements customer registration, HTTP Basic authentication, account creation/listing, and same-currency transfers using JPA and an in-memory H2 database. Other modules and endpoints below are planned; they are not implemented yet.

The design uses a **modular monolith**: features live in separate packages in one deployable application. Keep dependencies pointing inward from HTTP adapters toward application logic and domain rules. Avoid making one giant service or allowing one feature to reach into another feature's persistence layer.

## Proposed package structure

Keep the existing base package, `com.example.cyber`:

```text
src/main/java/com/example/cyber/
├── CyberApplication.java
├── config/
│   ├── SecurityConfiguration.java
│   ├── JwtProperties.java
│   └── CryptoConfiguration.java
├── common/
│   ├── api/                         # Shared API error response types
│   └── exception/                   # Shared exception mapping
├── auth/
│   ├── api/
│   │   ├── AuthController.java
│   │   └── dto/                      # RegisterRequest, LoginRequest, AuthResponse
│   ├── application/                  # RegisterUserUseCase, LoginUserUseCase, VerifyOtpUseCase
│   ├── domain/                       # User, authentication policies, domain exceptions
│   └── infrastructure/               # JPA entities, repositories, password/JWT adapters
├── account/
│   ├── api/                          # AccountController and response DTOs
│   ├── application/                  # CreateAccountUseCase, GetAccountUseCase
│   ├── domain/                       # BankAccount, Money, account rules
│   └── infrastructure/               # Persistence adapters
├── transaction/
│   ├── api/                          # TransactionController and request/response DTOs
│   ├── application/                  # DepositFunds, WithdrawFunds, TransferFunds use cases
│   ├── domain/                       # Transaction, transaction policies and events
│   └── infrastructure/               # Persistence and transaction-boundary adapters
├── security/
│   ├── crypto/                       # Encryption and key-management ports/adapters
│   ├── signature/                    # Transaction signing and verification
│   ├── fraud/                        # Fraud signals and decision policies
│   └── rate_limit/                   # Rate-limit policy and implementation
├── phishing/
│   ├── api/                          # PhishingController and URL DTOs
│   ├── application/                  # CheckUrlUseCase and risk aggregation
│   ├── domain/                       # PhishingRule, RuleResult, RiskLevel
│   └── infrastructure/               # Rule implementations and threat-feed adapters
└── audit/
    ├── application/                  # Audit recording port/use case
    ├── domain/                       # AuditEvent
    └── infrastructure/               # Audit persistence adapter
```

Within each feature, name packages for their role. Use `api`, `application`, `domain`, and `infrastructure` consistently; avoid generic top-level folders such as `service`, `repository`, and `entity` that mix unrelated features.

## Dependency direction and request flow

```text
HTTP request
    ↓
API controller and request validation
    ↓
Application use case
    ├── Domain rules and value objects
    ├── Ports for persistence, audit, crypto, and external services
    ↓
Infrastructure adapters implement those ports
    ↓
Database or external provider
```

Controllers translate HTTP into use-case inputs and map results to response DTOs. Use cases coordinate one business action. Domain objects enforce rules that must remain true regardless of HTTP or database technology. Infrastructure implements the interfaces owned by the application/domain boundary.

## Module responsibilities

### Authentication (`auth`)

Owns registration, login, MFA challenges, token refresh, and logout. Store passwords using a purpose-built password hashing function such as Argon2id or BCrypt; never encrypt passwords for later recovery. Keep access and refresh token lifetimes and revocation policy explicit. Return a short-lived authentication challenge after password verification when MFA is required; issue the normal access token only after successful MFA.

Implemented endpoint:

```text
POST /api/v1/auth/register
```

Planned endpoints:

```text
POST /api/v1/auth/login
POST /api/v1/auth/mfa/verify
POST /api/v1/auth/token/refresh
POST /api/v1/auth/logout
```

### Accounts (`account`)

Owns account creation, account status, account summaries, and balance reads. Derive the customer identity from the authenticated principal and check ownership or explicit privileges on every account access. Do not trust a caller-supplied user ID as authorization.

Implemented endpoints:

```text
POST /api/v1/accounts
GET  /api/v1/accounts
GET  /api/v1/accounts/{accountId}
```

### Transactions (`transaction`)

Owns deposits, withdrawals, transfers, and transaction history. Represent amounts with `BigDecimal` or a currency-aware `Money` value object; never use floating-point types for balances. Apply a database transaction to all related balance and ledger updates. Lock or otherwise serialize affected accounts consistently, validate sufficient funds, and use idempotency keys to prevent duplicate processing after retries. Record an immutable ledger entry; do not treat a mutable balance field as the sole financial record.

Implemented endpoint:

```text
POST /api/v1/transactions/transfers
```

Planned endpoints:

```text
POST /api/v1/transactions/deposits
POST /api/v1/transactions/withdrawals
GET  /api/v1/transactions/{transactionId}
GET  /api/v1/accounts/{accountId}/transactions
```

For a transfer, authorize and validate both accounts, validate MFA/step-up requirements and limits, then atomically persist the debit, credit, and ledger entries. Do not call a remote service while holding a database transaction open unless the integration explicitly requires it and has a recovery design.

### Cryptography (`security.crypto`, `security.signature`)

Keep cryptographic operations behind small interfaces owned by application features. Use authenticated encryption such as AES-GCM for recoverable sensitive data, with unique nonces and keys held in a KMS or secret manager. Never hard-code keys or log plaintext, keys, OTPs, or tokens.

Use digital signatures only when there is a defined signing protocol and key lifecycle. A signature proves possession of a private key over specific bytes; it does not replace authentication, authorization, transaction controls, or a database transaction. Define canonical transaction serialization, algorithm, key rotation, and verification before enabling signatures.

Suggested ports:

```java
public interface DataEncryptionPort {
    EncryptedValue encrypt(byte[] plaintext);
    byte[] decrypt(EncryptedValue encryptedValue);
}

public interface TransactionSigningPort {
    SignatureValue sign(byte[] canonicalTransaction);
    boolean verify(byte[] canonicalTransaction, SignatureValue signature);
}
```

Prefer typed values over ambiguous `String` parameters for ciphertext and signatures. Keep concrete algorithms in infrastructure adapters.

### Phishing checks (`phishing`)

Accept a URL, normalize and validate it, run independent `PhishingRule` implementations, and aggregate rule results into a risk level and reasons. A rule should return evidence/weight rather than decide the overall result. Keep rules injectable so each can be added or replaced without changing the aggregator.

Potential rules include suspicious hostname patterns, IP-literal hosts, deceptive subdomains, risky URL schemes, and threat-feed matches. Do not fetch arbitrary submitted URLs from the server without strict SSRF controls. A risk score is a heuristic, not a guarantee of safety.

Proposed endpoint:

```text
POST /api/v1/security/url-checks
```

Example response:

```json
{
  "safe": false,
  "riskScore": 85,
  "riskLevel": "HIGH",
  "reasons": ["Suspicious hostname", "Credential-related path"]
}
```

### Audit (`audit`)

Record security-relevant outcomes such as login success/failure, MFA outcomes, account status changes, and money movement. Capture actor, action, target, outcome, timestamp, and a correlation identifier where available. Minimize personal data and never include credentials, tokens, OTPs, encryption keys, or full payment secrets. Restrict audit read access and define retention and tamper-resistance requirements before production use.

Audit persistence should not silently disappear when required for a financial operation. Decide per event whether audit is part of the same database transaction or delivered through a transactional outbox; make failure behavior explicit.

### Cross-cutting security

Apply authentication, authorization, request validation, rate limits, and safe error mapping centrally where practical. Keep account lockout and fraud policies explicit and avoid revealing whether a username/account exists in unauthenticated responses. Store security events with enough context to investigate while limiting sensitive data.

## SOLID and naming rules

- **Single Responsibility:** one use case handles one action; controllers handle HTTP; adapters handle technology integration.
- **Open/Closed:** add phishing checks through new rule implementations and composition, not edits to a large conditional method.
- **Liskov Substitution:** implementations of a port must preserve its documented behavior and failure contract.
- **Interface Segregation:** define narrow ports such as `AccountRepository`, `AuditWriter`, and `DataEncryptionPort`; avoid one broad utility interface.
- **Dependency Inversion:** application logic depends on abstractions; Spring wires infrastructure implementations at the edge.

Java conventions:

- Types use `UpperCamelCase`: `TransferFundsUseCase`, `AccountController`.
- Methods and fields use `lowerCamelCase`: `transferFunds`, `availableBalance`.
- Constants use `UPPER_SNAKE_CASE`: `MAX_LOGIN_ATTEMPTS`.
- Use descriptive names (`accountId`, `transactionAmount`, `authenticationChallenge`) instead of vague names (`id`, `data`, `value`) when ambiguity is possible.
- Use request/response DTOs at the API boundary; do not serialize persistence entities directly.
- Prefer constructor injection and immutable fields. Avoid static mutable state and catch-all utility classes.
- Use domain-specific exceptions and map them centrally to stable, non-sensitive API errors.

## API and error conventions

Use versioned paths under `/api/v1`. Validate request shape and constraints before invoking a use case. Return stable error codes and safe messages; keep stack traces and internal details in server logs only. Consider a standard problem response (`application/problem+json`) with a correlation ID.

## Implementation sequence

1. Define domain types, persistence schema, and API error conventions.
2. Implement registration/login and password hashing.
3. Add MFA challenges, token lifecycle, and authorization rules.
4. Implement account ownership and account reads.
5. Implement deposits and withdrawals with a ledger and transactional guarantees.
6. Implement transfers, idempotency, and concurrency controls.
7. Add audit recording and operational security events.
8. Add encryption/key-management integration for specific recoverable fields.
9. Add signing only after the signing protocol and key lifecycle are defined.
10. Add phishing rules and risk aggregation.
11. Add rate limits and fraud signals, then tune policies using observed behavior.

Before exposing banking functionality to real users, the implementation needs security review, operational monitoring, backup/recovery plans, and applicable regulatory/compliance review.

