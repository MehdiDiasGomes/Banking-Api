# Banking API

> Secure, production-grade RESTful API for banking operations — built with Java 21 & Spring Boot 3.

[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()

---

## Overview

**Banking API** is a backend service simulating core banking operations: account management, fund transfers, transaction history, and multi-role authentication. Designed with security, scalability, and auditability in mind — principles that are non-negotiable in the financial industry.

This project demonstrates applied knowledge of:
- Stateless JWT-based authentication with role-based access control (RBAC)
- Domain-Driven Design (DDD) with a clean layered architecture
- Financial data integrity patterns (optimistic locking, idempotency)
- Comprehensive audit logging for regulatory traceability

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Security](#security)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Roadmap](#roadmap)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3 · Spring Security 6 |
| Authentication | JWT (RS256) · Spring Security OAuth2 |
| Persistence | Spring Data JPA · PostgreSQL |
| Migration | Flyway |
| Validation | Jakarta Bean Validation |
| Documentation | OpenAPI 3 / Springdoc |
| Testing | JUnit 5 · Mockito · Testcontainers |
| Build | Maven 3 |
| Containerization | Docker · Docker Compose |

---

## Architecture

The project follows a **layered DDD** approach with strict separation of concerns:

```
src/
└── main/
    └── java/com/bankingapi/
        ├── config/          # Security, CORS, beans configuration
        ├── domain/          # Entities, value objects, domain exceptions
        │   ├── account/
        │   ├── transaction/
        │   └── user/
        ├── application/     # Use cases, DTOs, mappers
        │   ├── account/
        │   ├── transaction/
        │   └── auth/
        ├── infrastructure/  # JPA repositories, external adapters
        └── api/             # REST controllers, request/response models
            ├── auth/
            ├── account/
            └── transaction/
```

**Key design principles:**
- Controllers handle only HTTP concerns — no business logic
- Domain entities are never exposed directly via the API
- All mutations go through use-case services
- Repository interfaces are defined in the domain layer, implemented in infrastructure

---

## Features

### Authentication & Authorization
- User registration and login with hashed passwords (BCrypt, cost factor 12)
- JWT access tokens (short-lived) + refresh token rotation
- Role-based access control: `ROLE_CUSTOMER`, `ROLE_ADVISOR`, `ROLE_ADMIN`
- Token blacklisting on logout

### Account Management
- Create and close bank accounts (checking, savings)
- Retrieve account details and balance
- Multi-currency support (EUR, USD, GBP)

### Transactions
- Peer-to-peer fund transfers with optimistic locking to prevent race conditions
- Idempotent transfer endpoint (idempotency key via request header)
- Paginated transaction history with date range filtering
- Automatic rollback on partial failures

### Audit & Compliance
- Immutable audit log for all state-changing operations
- Timestamps: `created_at`, `updated_at` on all entities
- Soft-delete pattern for regulatory data retention

---

## Security

Security is the primary concern for any financial application. This API implements:

| Control | Implementation |
|---|---|
| Password hashing | BCrypt (strength 12) |
| Token signing | RS256 (asymmetric key pair) |
| HTTPS enforcement | Redirect HTTP → HTTPS |
| Input validation | Jakarta Bean Validation on all DTOs |
| SQL injection prevention | JPA parameterized queries only |
| CORS | Explicit allow-list, no wildcard origins |
| Rate limiting | `Bucket4j` — per IP and per user |
| Sensitive data | Masked account numbers in API responses |

> Secrets (database credentials, JWT private key) are injected via environment variables — never hardcoded.

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/Banking-Api.git
cd Banking-Api
```

### 2. Configure environment variables

```bash
cp .env.example .env
# Edit .env with your local values
```

```env
DB_URL=jdbc:postgresql://localhost:5432/banking_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_PRIVATE_KEY=classpath:certs/private.pem
JWT_PUBLIC_KEY=classpath:certs/public.pem
```

### 3. Start dependencies

```bash
docker compose up -d   # starts PostgreSQL
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`. Interactive docs: `http://localhost:8080/swagger-ui.html`

---

## API Reference

> Full OpenAPI specification is auto-generated and served at `/v3/api-docs`.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Obtain access + refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token |
| `POST` | `/api/v1/auth/logout` | Invalidate current tokens |

### Accounts

| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/v1/accounts` | List all accounts of current user | CUSTOMER |
| `POST` | `/api/v1/accounts` | Open a new account | CUSTOMER |
| `GET` | `/api/v1/accounts/{id}` | Get account details | CUSTOMER |
| `DELETE` | `/api/v1/accounts/{id}` | Close an account | CUSTOMER |
| `GET` | `/api/v1/admin/accounts` | List all accounts | ADMIN |

### Transactions

| Method | Endpoint | Description | Role |
|---|---|---|---|
| `POST` | `/api/v1/transactions/transfer` | Initiate a fund transfer | CUSTOMER |
| `GET` | `/api/v1/transactions` | Get transaction history (paginated) | CUSTOMER |
| `GET` | `/api/v1/transactions/{id}` | Get transaction details | CUSTOMER |

#### Transfer request example

```json
POST /api/v1/transactions/transfer
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

{
  "sourceAccountId": "acc_123",
  "targetAccountId": "acc_456",
  "amount": 250.00,
  "currency": "EUR",
  "description": "Monthly rent"
}
```

---

## Testing

```bash
# Unit + integration tests
./mvnw test

# Integration tests only (requires Docker for Testcontainers)
./mvnw test -Pintegration
```

**Test coverage targets:**
- Domain layer: > 90%
- Application (use cases): > 85%
- API layer: contract tests via MockMvc

---

## Roadmap

- [ ] IBAN generation (ISO 13616 compliant)
- [ ] Scheduled recurring transfers
- [ ] Two-factor authentication (TOTP)
- [ ] Webhook notifications on transaction events
- [ ] Prometheus metrics + Grafana dashboard
- [ ] Kubernetes deployment manifests (Helm chart)

---

## License

Distributed under the MIT License. See [`LICENSE`](./LICENSE) for details.

---

<p align="center">
  Built with focus on security, reliability, and clean architecture.
</p>
