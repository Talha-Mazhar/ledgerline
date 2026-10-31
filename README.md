# Ledgerline

[![CI](https://github.com/Talha-Mazhar/ledgerline/actions/workflows/ci.yml/badge.svg)](https://github.com/Talha-Mazhar/ledgerline/actions/workflows/ci.yml)

A multi-currency wallet and payments app built on a double-entry ledger.
Users sign up, open wallets in EUR, USD, CZK and GBP, add and withdraw money,
exchange between currencies and send money to each other. Every movement of
money is a balanced, append-only journal entry.

| Part     | Stack                                                                           |
| -------- | ------------------------------------------------------------------------------- |
| Backend  | Java 25, Spring Boot 4.1, Spring Modulith, Spring Security, JPA + JdbcClient, PostgreSQL 18, Flyway |
| Frontend | React 19, TypeScript (strict), Vite, MUI, TanStack Query, React Router, React Hook Form + zod |
| API      | OpenAPI 3 contract; typed client and zod schemas generated from it              |
| Quality  | Error Prone + NullAway, Spotless, Testcontainers, contract-checked API tests, oxlint, Vitest + MSW |
| Delivery | Docker (multi-stage images, nginx), Docker Compose, GitHub Actions              |

## What it does

- **Accounts.** Sign-up and sign-in with server sessions in `HttpOnly` cookies
  and CSRF protection ([ADR 0007](docs/adr/0007-sessions-and-csrf.md)).
- **Wallets.** One per currency; the balance is derived from the ledger, never
  stored twice ([ADR 0005](docs/adr/0005-wallets-design.md)).
- **Add money and withdraw.** Simulated card top-ups and bank payouts.
- **Exchange.** Live quotes at Ledgerline's rate (mid-market minus a 0.5%
  spread); the bought amount is rounded down, in Ledgerline's favour, as banks do.
- **Send money.** To another user by email, into their wallet in the same
  currency.
- **Activity.** Every wallet's ledger lines with the running balance.

Under the hood ([ADR 0006](docs/adr/0006-double-entry-ledger.md)):
double-entry postings that must sum to zero per currency (checked by the
database at commit), append-only tables, row locks in a fixed order against
double spending, and idempotency keys so a retried request moves money once.

## Try it

The fastest way: everything in containers.

```bash
docker compose up --build
```

Open http://localhost:8000 and choose **Try the demo account**, or sign in as
`demo@ledgerline.dev` with the password `ledgerline-demo`. Send money to the
second demo account, `jana@ledgerline.dev` (same password), which has EUR and
CZK wallets. No real money moves anywhere.

## Develop

Prerequisites: JDK 25, Node.js 24, Docker Desktop.

```bash
# backend: starts PostgreSQL from infra/compose.yaml, then the API on :8080
cd backend && ./mvnw spring-boot:run

# frontend: http://localhost:5173 (forwards /api to :8080)
cd frontend && npm install && npm run dev
```

Checks, the same as CI runs:

```bash
cd backend && ./mvnw verify     # null checks, tests on real PostgreSQL, module boundaries, formatting
cd frontend && npm run check    # types, lint, formatting, tests
```

## Layout

```
backend/    Spring Boot application, one package per module:
            identity, wallets, ledger, payments, fx
frontend/   React app, one folder per feature
api/        OpenAPI contract, the source of the types on both sides
infra/      PostgreSQL for development
docs/adr/   architecture decisions: what we chose, the options and why
compose.yaml    the whole app in containers
```

## The API

`api/openapi.yaml` is the contract between backend and frontend. Change it
first, then:

```bash
cd frontend && npm run api:generate   # regenerate the typed client
cd backend && ./mvnw verify           # the API tests check every response against it
```

CI fails if the committed client doesn't match the contract.

## Decisions

- [0001 Backend as a modular monolith](docs/adr/0001-modular-monolith.md)
- [0002 Null safety checked at compile time](docs/adr/0002-null-safety.md)
- [0003 Feature-based frontend structure](docs/adr/0003-frontend-structure.md)
- [0004 Contract-first API, checked on both sides](docs/adr/0004-contract-first-api.md)
- [0005 Wallets: ownership, balances and identity before sign-in](docs/adr/0005-wallets-design.md)
- [0006 A double-entry ledger, written in SQL](docs/adr/0006-double-entry-ledger.md)
- [0007 Sign-in with server sessions and CSRF tokens, not JWTs](docs/adr/0007-sessions-and-csrf.md)
