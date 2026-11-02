# Ledgerline

[![CI](https://github.com/Talha-Mazhar/ledgerline/actions/workflows/ci.yml/badge.svg)](https://github.com/Talha-Mazhar/ledgerline/actions/workflows/ci.yml)
[![Security](https://github.com/Talha-Mazhar/ledgerline/actions/workflows/security.yml/badge.svg)](https://github.com/Talha-Mazhar/ledgerline/actions/workflows/security.yml)

A multi-currency wallet and payments app built on a double-entry ledger. Customers sign up, open
wallets in EUR, USD, CZK and GBP, add and withdraw money, exchange at live rates and send money to
each other. Large or unusual payments wait for two members of staff to approve them. Every movement
of money is a balanced, append-only journal entry.

| Part          | Stack                                                                                       |
| ------------- | ------------------------------------------------------------------------------------------- |
| Backend       | Java 25, Spring Boot 4.1, Spring Modulith, Spring Security, JPA + JdbcClient, PostgreSQL 18, Flyway |
| Messaging     | Kafka (transactional outbox, retry and dead-letter topics), Redis (sessions, cache), ShedLock |
| Integrations  | S3 / RustFS (documents, statements), Resilience4j (rate feed), Spring AI + Ollama, PDFBox, SMTP |
| Frontend      | React 19, TypeScript (strict), Vite, MUI, TanStack Query, React Router, React Hook Form + zod |
| API           | OpenAPI 3 contract; typed client and zod schemas generated from it                          |
| Quality       | Error Prone + NullAway, Testcontainers, contract-checked API tests, Vitest + MSW, Playwright, Gatling |
| Operations    | Micrometer + Prometheus, OpenTelemetry + Tempo, Grafana, Docker, Helm, GitHub Actions, CodeQL, Trivy |

## What it does

**For customers**

- **Accounts.** Sign-up and sign-in with server sessions in `HttpOnly` cookies and CSRF protection
  ([ADR 0007](docs/adr/0007-sessions-and-csrf.md)).
- **Wallets.** One per currency; the balance is derived from the ledger, never stored twice.
- **Add money, withdraw, exchange, send.** Exchange at the ECB rate minus a 0.5% spread, with a
  live quote; send money to another user by email.
- **Payments.** Each payment with its status. A payment above the customer's limits, too many in a
  short time, or a large first payment to someone new is **held**: the money is set aside and the
  customer sees why ([ADR 0008](docs/adr/0008-payments-state-machine-and-four-eyes.md)).
- **Identity & limits.** Upload an ID document; once staff approve it, limits go up
  ([ADR 0010](docs/adr/0010-identity-documents-and-tiers.md)).
- **Notifications** in the app and by email, a moment after each payment
  ([ADR 0009](docs/adr/0009-events-outbox-and-kafka.md)).
- **Statements.** A PDF per wallet per month, built from the ledger.
- **Assistant.** "Where did my money go this month?" answered by a model running on Ledgerline's
  own servers, with read-only access to the customer's own data
  ([ADR 0013](docs/adr/0013-assistant-with-a-local-model.md)).

**For staff** (back office)

- **Approvals.** Held payments need two approvals from two different people; reject with a reason,
  or refund a completed payment.
- **Verifications.** Review uploaded identity documents.
- **Users.** Look people up; admins change roles.
- **Audit log.** Every staff decision, append-only.

Under the hood ([ADR 0006](docs/adr/0006-double-entry-ledger.md)): postings that must sum to zero
per currency (checked by the database at commit), append-only tables, row locks in a fixed order
against double spending, idempotency keys so a retried request moves money once, and a test that
runs 100 transfers in parallel and checks the books still balance.

## Try it

Everything in containers:

```bash
docker compose up --build
```

Open http://localhost:8000 and choose **Try the demo account**. Other accounts, all with the
password `ledgerline-demo`:

| Account                  | Role                                          |
| ------------------------ | --------------------------------------------- |
| `demo@ledgerline.dev`    | Customer                                      |
| `jana@ledgerline.dev`    | Customer with EUR and CZK wallets, to send to |
| `support@ledgerline.dev` | Support: approvals, verifications             |
| `admin@ledgerline.dev`   | Admin: also changes roles                     |

Send more than €1,000 as a customer, then approve it as support and as admin. Emails the app sends
are at http://localhost:8025 (Mailpit). No real money moves anywhere.

More of the stack:

```bash
docker compose --profile observability up --build   # + Grafana http://localhost:3000 (admin/admin)
docker compose --profile ai up --build               # + the assistant's model (about 2 GB)
```

## Deploy to Kubernetes

```bash
helm install ledgerline deploy/helm/ledgerline                 # with in-cluster data stores
helm install ledgerline deploy/helm/ledgerline \
  -f deploy/helm/ledgerline/values-production.yaml             # with managed services
```

`values-kind.yaml` runs it on a local [kind](https://kind.sigs.k8s.io) cluster; CI does exactly
that on every push. See [ADR 0012](docs/adr/0012-observability-and-deployment.md).

## Develop

Prerequisites: JDK 25, Node.js 24, Docker.

```bash
# backend: starts PostgreSQL, Kafka, Redis, RustFS and Mailpit from infra/compose.yaml, then the API on :8080
cd backend && ./mvnw spring-boot:run

# frontend: http://localhost:5173 (forwards /api to :8080)
cd frontend && npm install && npm run dev
```

Checks, the same as CI runs:

```bash
cd backend && ./mvnw verify         # null checks, tests on real PostgreSQL/Kafka/Redis/S3, module boundaries, formatting
cd frontend && npm run check        # types, lint, formatting, tests
cd e2e && npx playwright test       # end-to-end, against `docker compose up`
cd loadtest && ./mvnw gatling:test  # load test, against `docker compose up`
```

## Layout

```
backend/     Spring Boot application, one package per module: identity, wallets, ledger, payments,
             fx, risk, onboarding, documents, messaging, notifications, audit, statements, assistant
frontend/    React app, one folder per feature
api/         OpenAPI contract, the source of the types on both sides
e2e/         Playwright tests of the whole app
loadtest/    Gatling simulation
deploy/      Helm chart
infra/       development services; Prometheus, Tempo and Grafana configuration
docs/        architecture decisions, performance report
compose.yaml the whole app in containers
```

## Decisions

- [0001 Backend as a modular monolith](docs/adr/0001-modular-monolith.md)
- [0002 Null safety checked at compile time](docs/adr/0002-null-safety.md)
- [0003 Feature-based frontend structure](docs/adr/0003-frontend-structure.md)
- [0004 Contract-first API, checked on both sides](docs/adr/0004-contract-first-api.md)
- [0005 Wallets: ownership, balances and identity before sign-in](docs/adr/0005-wallets-design.md)
- [0006 A double-entry ledger, written in SQL](docs/adr/0006-double-entry-ledger.md)
- [0007 Sign-in with server sessions and CSRF tokens, not JWTs](docs/adr/0007-sessions-and-csrf.md)
- [0008 Payments as a state machine, with four-eyes approval](docs/adr/0008-payments-state-machine-and-four-eyes.md)
- [0009 Events through a transactional outbox to Kafka](docs/adr/0009-events-outbox-and-kafka.md)
- [0010 Identity documents in S3, tiers and staff review](docs/adr/0010-identity-documents-and-tiers.md)
- [0011 Exchange rates: retries, a circuit breaker, a cache and one scheduler](docs/adr/0011-resilience-and-caching.md)
- [0012 Observability, and deployment to Kubernetes with Helm](docs/adr/0012-observability-and-deployment.md)
- [0013 "Where did my money go?" with a local model and read-only tools](docs/adr/0013-assistant-with-a-local-model.md)

[Changelog](CHANGELOG.md) · [Security](SECURITY.md) · [Performance](docs/performance.md)
