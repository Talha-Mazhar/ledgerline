# 0012. Observability, and deployment to Kubernetes with Helm

- Status: accepted
- Date: 2026-11-02

## Context

A payments service has to show what it is doing (how many payments, how fast, how many held) and
be deployable the same way everywhere, from a laptop to a production cluster.

## Decision

**Observability.**

- Micrometer metrics, scraped by Prometheus from `/actuator/prometheus`. Besides the standard HTTP,
  JVM, connection pool and Kafka metrics, the app counts payments by kind and status
  (`ledgerline_payments_total`) and times ledger postings (`ledgerline_ledger_posts`).
- Traces with OpenTelemetry, exported over OTLP to Tempo; Spring Modulith adds a span per module
  call, so a trace shows a payment crossing modules.
- Actuator listens on its **own port (8081)**, which the ingress never routes, so health and
  metrics are never public.
- A Grafana dashboard (`infra/observability`) shows request rate and p95 latency per endpoint,
  server errors, payments by kind and status, ledger post time, refusals for insufficient funds,
  Kafka consumer lag, database connections, heap and CPU. `docker compose
  --profile observability up` starts the whole stack.

**Deployment.**

- Two images: the backend (layered jar on a JRE, non-root) and the frontend (nginx-unprivileged,
  non-root, with security headers and a Content-Security-Policy). Both run with read-only root
  file systems.
- A **Helm chart** (`deploy/helm/ledgerline`): probes on the management port, a rolling update
  that never drops below the wanted replicas, autoscaling on CPU, pod disruption budgets, spread
  over nodes, network policies (only the frontend reaches the API; only the backend reaches the
  data stores), Ingress or Gateway API, and an optional ServiceMonitor.
- `infrastructure.enabled` runs PostgreSQL, Kafka, Redis, RustFS and Mailpit in the cluster for a
  demo. `values-production.yaml` turns them off and points at managed services (RDS, MSK,
  ElastiCache, S3, SES): their replication and backups are better than anything a single chart
  would run.
- CI installs the chart on a kind cluster and signs in through it. A release tag publishes
  multi-architecture images with an SBOM and provenance attestation, and the chart as an OCI
  artifact.

## Consequences

Running it for real costs money (a managed cluster and services); the chart and CI prove it
deploys, and the same chart deploys to production with a different values file.
