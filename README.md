# Secure Banking Backend

A Spring Boot banking backend starter with customer registration, account creation/listing, and account-to-account transfers. Other modules in [ARCHITECTURE.md](docs/ARCHITECTURE.md) are planned extension points.

## Technology

- Java 21
- Spring Boot (version managed in `pom.xml`)
- Maven Wrapper
- Spring MVC, Spring Security, and Spring Data MongoDB

## Getting started

### Prerequisites

- JDK 21
- Git (optional, for checkout/version control)

### Run the application

Set the MongoDB Atlas connection string in `MONGODB_URI` before starting the application. Use the URI you provided, and rotate its password if it has been shared in an exposed channel.

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The application listens on port `8080` by default (or the `PORT` value supplied by Render) and persists customers, accounts, and transfers in MongoDB. Protected endpoints currently use HTTP Basic with the registered email and password. JWT authentication is not implemented yet; `JWT_SECRET` is reserved for that addition. Use HTTPS outside local development. Transfers require an Atlas replica set so MongoDB multi-document transactions are available.

Account and transaction endpoints require HTTP Basic authentication with the registered email and password. New-device email verification is disabled.

### Deploy on Render with Docker

Create a **Web Service** from this repository and choose **Docker** as the runtime. Render will build the root `Dockerfile` and provide `PORT` at runtime. Add these environment variables in the Render service settings:

- `MONGODB_URI`: your MongoDB Atlas connection string.
- `JWT_SECRET`: a randomly generated secret of at least 32 bytes, even though JWT authentication is not active yet.
- `TRANSACTION_SIGNING_PRIVATE_KEY_BASE64`: Base64-encoded PKCS#8 P-256 private key. Keep this secret.
- `TRANSACTION_SIGNING_PUBLIC_KEY_BASE64`: Base64-encoded X.509 P-256 public key.
- `TRANSACTION_SIGNING_KEY_ID`: key identifier, for example `northstar-p256-v1`.
- `CORS_ALLOWED_ORIGINS`: optional comma-separated frontend origins allowed to call the API. Include the scheme and port if needed, and do not add a trailing slash. Defaults include local development and `https://cyber-frontend-nu.vercel.app`.

Keep secrets in Render's environment settings; do not put them in the Dockerfile or commit them to Git. Set Atlas network access to permit connections from the Render service. Use a replica set compatible with MongoDB transactions for transfers.

Generate an ECDSA P-256 key pair locally with OpenSSL, then copy only the Base64 outputs into Render environment variables:

```bash
openssl ecparam -name prime256v1 -genkey -noout -out signing-key.pem
openssl pkcs8 -topk8 -nocrypt -in signing-key.pem -outform DER -out signing-private.der
openssl pkey -in signing-key.pem -pubout -outform DER -out signing-public.der
```

In PowerShell, Base64-encode the DER files with `[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\signing-private.der"))` and the equivalent `signing-public.der` command. Store the private-key output as a Render secret. Do not commit `signing-key.pem` or either DER file. Money movement fails closed with HTTP 503 until a matching key pair is configured.

## Available API

Register a customer (public):

```http
POST /api/v1/auth/register
Content-Type: application/json

{"email":"person@example.com","password":"use-a-long-unique-password"}
```

Create and list accounts (HTTP Basic authentication required; accounts are scoped to the authenticated customer):

```http
POST /api/v1/accounts
GET  /api/v1/accounts
GET  /api/v1/accounts/{accountId}

{"currency":"INR","initialAmount":30000.00}
```

`initialAmount` is optional and defaults to `0.00`. It accepts non-negative amounts with up to two decimal places. A nonzero opening amount is recorded as a deposit in transaction history. This is a demo balance feature and does not move real money.

Include the `Authorization` header with each account request.

Transfers, deposits, withdrawals, transaction history, and transaction verification require HTTP Basic authentication. Supply a stable, unique `Idempotency-Key` for each intended money movement and reuse it for retries:

```http
POST /api/v1/transactions/transfers
Idempotency-Key: 8b3ed41a-49e1-4635-9bb3-e7bc0c0e7f08

{
  "sourceAccountId":"<source UUID>",
  "destinationAccountId":"<destination UUID>",
  "amount":100.00
}
```

Each successful money-movement response also contains `signatureAlgorithm`, `signingKeyId`, and `signature` (Base64-encoded ECDSA signature).

```http
POST /api/v1/transactions/deposits
POST /api/v1/transactions/withdrawals
GET  /api/v1/transactions

{"accountId":"<account UUID>","amount":25.00}
```

The deposit route directly credits a demo balance. It is not connected to a funding provider and must not be exposed as a real-money deposit flow. URL risk checks are available at `POST /api/v1/security/url-checks` with `{"url":"https://example.com"}`; they are heuristic and do not fetch or guarantee the safety of a URL. Five failed authentication attempts temporarily lock the customer account for 15 minutes.

```http
GET /api/v1/security/transaction-signing-key
GET /api/v1/transactions/{transactionId}/verify
```

The public-key endpoint returns `algorithm`, `keyId`, and `publicKeyBase64`. The verification endpoint returns `transactionId`, `valid`, `signatureAlgorithm`, and `signingKeyId`.

Every new transfer, deposit, and withdrawal is signed by the backend using ECDSA P-256 with SHA-256 (`SHA256withECDSA`). The canonical signed fields are transaction version, ID, type, source account ID, destination account ID, initiating customer, amount normalized to two decimal places, currency, and creation time. The transaction response includes the Base64 signature, algorithm, and key ID. Fetch the public key at `GET /api/v1/security/transaction-signing-key`; verify a stored transaction at `GET /api/v1/transactions/{transactionId}/verify` (authenticated and limited to the initiating customer). This proves the configured backend key signed those transaction fields; it is not an individual customer's private-key signature. Keep the private key secret. Verification uses the currently configured key pair, so key rotation needs a trusted key history before old signatures can continue to verify.

Transfer, deposit, and withdrawal retries are idempotent: repeating a request with the same key and same details returns the original response; reusing that key for different details returns HTTP 409. Account writes use optimistic version checks and return HTTP 409 if a concurrent update wins. The deposit route is a demo credit operation; production deposits require a trusted funding provider and reconciliation. TOTP/MFA, JWT and refresh tokens, recovery, beneficiary controls, managed encryption keys, fraud review workflows, and production audit/monitoring are not implemented. Do not use this starter to hold real funds.

Account endpoints require authentication and only expose accounts owned by the authenticated customer.

### Package the application

```powershell
.\mvnw.cmd package
```

### Run the API tests

```powershell
.\mvnw.cmd test
```

The transfer integration test creates a funded account fixture, calls the transfer API, and checks both resulting balances.

## Project layout

```text
.
├── docs/
│   └── ARCHITECTURE.md      # Planned modules, responsibilities, and conventions
├── src/
│   ├── main/
│   │   ├── java/com/example/cyber/
│   │   │   ├── auth/
│   │   │   ├── account/
│   │   │   ├── transaction/
│   │   │   ├── config/
│   │   │   └── CyberApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/com/example/cyber/
├── mvnw
├── mvnw.cmd
└── pom.xml
```

As features are added, keep code inside the existing base package `com.example.cyber` so Spring component scanning can find it. See the architecture document for the proposed feature-package layout and naming rules.

## Configuration and secrets

Use environment variables or a secret manager for database credentials, JWT signing keys, encryption keys, and other secrets. Set `MONGODB_URI` to the MongoDB connection string. Do not commit real credentials or production keys to `application.properties`.

## Development guidance

- Keep controllers focused on HTTP input/output; put use-case behavior in services.
- Validate request data at the boundary and authorize access to every account and transaction.
- Use database transactions for money movement and enforce idempotency for retryable operations.
- Store passwords with a password-hashing algorithm; encryption is not a substitute for hashing.
- Use authenticated encryption (such as AES-GCM) for data that must be recoverable, with keys managed outside the database.
- Treat URL risk checks as advisory signals, not proof that a URL is safe.

## Documentation

- [Architecture and implementation plan](docs/ARCHITECTURE.md)

