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

### Deploy on Render with Docker

Create a **Web Service** from this repository and choose **Docker** as the runtime. Render will build the root `Dockerfile` and provide `PORT` at runtime. Add these environment variables in the Render service settings:

- `MONGODB_URI`: your MongoDB Atlas connection string.
- `JWT_SECRET`: a randomly generated secret of at least 32 bytes, even though JWT authentication is not active yet.
- `CORS_ALLOWED_ORIGINS`: optional comma-separated frontend origins allowed to call the API. Include the scheme and port if needed, and do not add a trailing slash. Defaults include local development and `https://cyber-frontend-nu.vercel.app`.

Keep both values in Render's environment settings; do not put secrets in the Dockerfile or commit them to Git. Set Atlas network access to permit connections from the Render service. Use a replica set compatible with MongoDB transactions for transfers.

## Available API

Register a customer (public):

```http
POST /api/v1/auth/register
Content-Type: application/json

{"email":"person@example.com","password":"use-a-long-unique-password"}
```

Create and list accounts (public, no login required):

```http
POST /api/v1/accounts
GET  /api/v1/accounts
GET  /api/v1/accounts/{accountId}

{"currency":"INR"}
```

Transfer between accounts (HTTP Basic authentication required; the source must belong to the authenticated customer; both accounts must use the same currency):

```http
POST /api/v1/transactions/transfers

{
  "sourceAccountId":"<source UUID>",
  "destinationAccountId":"<destination UUID>",
  "amount":100.00
}
```

Deposits are omitted because real deposits must reconcile with a trusted funding provider. MFA, refresh tokens, idempotency keys, a production database, and a complete immutable ledger are also not implemented. Do not use this starter to hold real funds.

Account endpoints are public. All callers share the same `public` account owner, so anyone can create, list, and view those accounts. This is suitable only for a demo; do not deploy it with real or sensitive account data.

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

