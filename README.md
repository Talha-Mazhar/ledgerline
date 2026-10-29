# Ledgerline

A multi-currency wallet and payments platform built on a double-entry ledger.
Users hold wallets in EUR, USD, CZK and GBP, top them up, send money to each
other and exchange currencies. Every movement of money is a balanced journal
entry.

| Part     | Stack                                                                    |
| -------- | ------------------------------------------------------------------------ |
| Backend  | Java 25, Spring Boot 4.1, Spring Modulith, JPA, PostgreSQL 18, Flyway     |
| Frontend | React 19, TypeScript (strict), Vite, MUI, TanStack Query, React Router   |
| Quality  | Error Prone + NullAway, Spotless, Testcontainers, oxlint, Vitest         |

## Layout

```
backend/    Spring Boot application (open in IntelliJ IDEA)
frontend/   React app (open in VS Code)
api/        OpenAPI contract, the source of the types on both sides
infra/      Docker Compose for local PostgreSQL
docs/adr/   architecture decisions: what we chose, the options and why
```

## Run it

Prerequisites: JDK 25, Node.js 24, Docker Desktop.

```bash
# backend: starts PostgreSQL from infra/compose.yaml, then the API on :8080
cd backend && ./mvnw spring-boot:run

# frontend: http://localhost:5173
cd frontend && npm install && npm run dev
```

Checks, the same as CI will run:

```bash
cd backend && ./mvnw verify     # compile with null checks, tests, formatting
cd frontend && npm run check    # types, lint, formatting, tests
```

## Decisions

- [0001 Backend as a modular monolith](docs/adr/0001-modular-monolith.md)
- [0002 Null safety checked at compile time](docs/adr/0002-null-safety.md)
- [0003 Feature-based frontend structure](docs/adr/0003-frontend-structure.md)
