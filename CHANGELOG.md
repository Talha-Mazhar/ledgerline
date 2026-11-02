# Changelog

All notable changes, newest first. Versions follow [Semantic Versioning](https://semver.org);
the format follows [Keep a Changelog](https://keepachangelog.com).

## [1.0.0] - 2026-11-02

Production-ready.

### Added

- Web app for everything below: a payments page with each payment's status, a notifications bell,
  identity verification with limits, monthly statements, the assistant, and the back office for
  staff (approvals, identity documents, users and roles, audit log). Navigation follows the role.
- Metrics (Micrometer, Prometheus) including payments by status and ledger post time, OpenTelemetry
  traces to Tempo, and a Grafana dashboard; actuator on its own port.
- Helm chart for Kubernetes: probes, autoscaling, disruption budgets, network policies, non-root
  read-only containers, Ingress or Gateway API, in-cluster or managed data stores.
- CI deploys the chart to kind and signs in through it, runs Playwright end-to-end tests against
  the app in Compose, and scans with CodeQL and Trivy. Releases publish multi-arch images with an
  SBOM and provenance, and the chart as an OCI artifact.
- Gatling load test and a performance report.
- Sessions in Redis, so any replica serves any user.

## [0.4.0] - 2026-11-02

### Added

- Live ECB exchange rates with retries, a circuit breaker, a Redis cache and a ShedLock-guarded
  hourly refresh.
- Monthly PDF statements, built from the ledger and archived to object storage.
- "Where did my money go?": an assistant on a local model (Ollama) with read-only tools.

## [0.3.0] - 2026-11-02

### Added

- Payments with a state machine: held payments, four-eyes approval by two different members of
  staff, rejection with a reason, refunds.
- Risk rules: per-tier single-payment and daily limits, velocity, first large payment to a new
  recipient.
- Domain events through a transactional outbox to Kafka, with retry topics and dead-letter topics;
  no event is lost while Kafka is down.
- Email and in-app notifications; an append-only audit log.
- Identity verification: documents to S3-compatible storage, checked by content, reviewed by staff.

## [0.2.0] - 2026-11-02

### Added

- Wallets in four currencies on a double-entry ledger that sums to zero per currency, checked by
  the database at commit and by a test of 100 parallel transfers.
- Add money, withdraw, exchange with a live quote, send money to other users, with idempotency
  keys.
- Sign-up and sign-in with server sessions and CSRF protection; customer, support and admin roles.
- Docker images, Compose and CI.

## [0.1.0] - 2026-10-30

### Added

- Walking skeleton: Spring Boot modular monolith with null checks at compile time, React app shell,
  OpenAPI contract, PostgreSQL in Testcontainers.

[1.0.0]: https://github.com/Talha-Mazhar/ledgerline/compare/v0.4.0...v1.0.0
[0.4.0]: https://github.com/Talha-Mazhar/ledgerline/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/Talha-Mazhar/ledgerline/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Talha-Mazhar/ledgerline/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Talha-Mazhar/ledgerline/releases/tag/v0.1.0
