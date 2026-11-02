# 0011. Exchange rates: retries, a circuit breaker, a cache and one scheduler

- Status: accepted
- Date: 2026-11-01

## Context

Rates come from an outside service (the ECB's reference rates via Frankfurter). It can be slow or
down; Ledgerline must still price exchanges, and must not hammer a service that is struggling. With
several replicas, a scheduled refresh would run once per replica.

## Decision

- **Resilience4j** around the feed: up to three tries, then a **circuit breaker** that opens when
  half of the last four calls failed and stays open for five minutes before letting a call through
  again. Meanwhile the app keeps using the last stored rates, which carry their timestamp.
- Fetched rates are stored in PostgreSQL (the history is useful for statements and audits) and
  read through a **Redis cache** (one hour), evicted when new rates are stored, so every replica
  sees the same rates without each asking the database.
- **ShedLock** makes the hourly refresh and the other scheduled jobs (outbox resubmission, monthly
  statements) run on one replica at a time, using a lock row in PostgreSQL.
- Sessions are also in Redis (Spring Session), so any replica serves any signed-in user and a
  rolling deploy signs nobody out.

## Consequences

Redis is now required. It holds nothing that can't be rebuilt: losing it signs users out and
empties the cache.
