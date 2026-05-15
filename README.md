# Banking API

> Secure RESTful API for core banking operations — built with Java 21 & Spring Boot 3.

[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

---

## Overview

**Banking API** is a backend service covering the core operations of a digital bank: user authentication, account management, deposits, and peer-to-peer fund transfers. Security and data integrity are the primary design drivers.

Key design choices:
- **JWT delivered as HttpOnly cookie** — prevents XSS token theft, unlike Bearer header approaches
- **`@Transactional` transfers with audit trail** — every transfer is recorded as `PENDING` before execution and marked `COMPLETED` or `FAILED`, ensuring full traceability
- **Ownership enforcement on every operation** — users can only read or mutate their own accounts
- **Stateless session management** — no server-side session state, horizontally scalable

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 · Spring Security 6 |
| Authentication | JWT (HMAC-SHA256) via JJWT 0.12 · HttpOnly cookie |
| Persistence | Spring Data JPA · PostgreSQL 16 |
| Validation | Jakarta Bean Validation |
| API Documentation | OpenAPI 3 / Springdoc 2.8 |
| Testing | JUnit 5 · Mockito · H2 (in-memory) |
| Build | Maven 3 |
| Dev tooling | Docker Compose · Adminer |

---

## Architecture

Layered architecture with strict separation of concerns:

```
src/main/java/com/mehdi/banking_api/
├── config/          # OpenAPI configuration
├── security/        # JwtService, JwtAuthFilter, SecurityConfig
├── model/           # JPA entities: User, Account, Transaction (+ enums)
├── dto/
│   ├── request/     # CreateAccountRequest, TransferRequest, LoginRequest…
│   └── response/    # AccountResponse, TransactionResponse, AuthResponse…
├── repository/      # Spring Data JPA interfaces
├── service/         # Business logic: AuthService, AccountService, TransactionService…
├── controller/      # REST endpoints: AuthController, AccountController, TransactionController…
├── exception/       # GlobalExceptionHandler, BusinessException, ForbiddenException…
└── common/          # ApiResult<T> response wrapper
```

**Rules enforced across layers:**
- Controllers resolve the authenticated user from `SecurityContextHolder` and delegate everything else to services
- Services own all business rules (ownership checks, balance validation, audit logging)
- JPA entities are never returned directly — always mapped to DTOs

---

## Features

### Authentication
- User registration and login — JWT signed with HMAC-SHA256, 24 h expiry
- Token delivered as an **HttpOnly + Secure cookie** (not exposed to JavaScript)
- Logout clears the cookie server-side
- Passwords hashed with BCrypt

### Account Management
- Create bank accounts (CHECKING / SAVINGS)
- List all accounts belonging to the authenticated user
- Deposit funds into an owned account (by IBAN)
- IBAN auto-generated on account creation

### Transactions
- **Fund transfer** between any two accounts by IBAN
- Sender account must belong to the authenticated user (enforced in service layer)
- Insufficient balance raises a `BusinessException` (400)
- Transfer wrapped in `@Transactional`: balance update + audit record are atomic
- **Audit trail**: every transfer is persisted as `PENDING` → `COMPLETED` | `FAILED`
- **Transaction history** for any owned account, queried by IBAN

### Error Handling
- Centralized `GlobalExceptionHandler` — consistent JSON error responses
- Custom exception hierarchy: `BusinessException` (400), `ForbiddenException` (403), `ResourceNotFoundException` (404)

---

## API Reference

> Full interactive documentation available at `http://localhost:8080/swagger-ui.html` once the app is running.

### Authentication — `/api/auth`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create account, sets JWT cookie |
| `POST` | `/api/auth/login` | Authenticate, sets JWT cookie |
| `POST` | `/api/auth/logout` | Invalidate JWT cookie |

### Accounts — `/api/accounts` *(requires authentication)*

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/accounts` | List authenticated user's accounts |
| `POST` | `/api/accounts` | Create a new account |
| `POST` | `/api/accounts/{iban}/deposit` | Deposit funds into an account |

### Transactions — `/transactions` *(requires authentication)*

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/transactions/history?iban=` | Transaction history for an owned account |
| `POST` | `/transactions/transfer` | Transfer funds between two accounts |

#### Transfer example

```json
POST /transactions/transfer

{
  "senderIban": "LU17786648221523",
  "receiverIban": "LU98123456789012",
  "amount": 250.00
}
```

#### Response envelope

All transaction and history responses are wrapped in a consistent `ApiResult<T>` envelope:

```json
{
  "data": { ... },
  "success": true
}
```

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. Clone the repository

```bash
git clone https://github.com/MehdiDiasGomes/Banking-Api.git
cd Banking-Api
```

### 2. Configure environment variables

The application expects the JWT secret to be provided via environment variable — never committed to source control.

```bash
export JWT_SECRET=your_base64_encoded_secret_here
```

Or create a local `application-local.properties` (git-ignored):

```properties
jwt.secret=your_base64_encoded_secret_here
```

### 3. Start the database

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16** on port `5432`
- **Adminer** (DB UI) on port `8081` → `http://localhost:8081`

### 4. Run the application

```bash
./mvnw spring-boot:run
```

API: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Testing

```bash
./mvnw test
```

Tests run against an **H2 in-memory database** (no Docker required). The test profile is configured in `src/test/resources/application-test.properties`.

Test coverage includes:
- Unit tests for services (`AuthServiceTest`, `AccountServiceTest`, `TransactionServiceTest`)
- Unit tests for security (`JwtServiceTest`)
- Integration tests for all controllers (`AuthControllerIntegrationTest`, `AccountControllerIntegrationTest`, `TransactionControllerIntegrationTest`, `UserControllerIntegrationTest`)

---

## License

Distributed under the MIT License. See [`LICENSE`](./LICENSE) for details.
