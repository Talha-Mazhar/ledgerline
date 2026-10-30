# 0001. Backend as a modular monolith

- Status: accepted
- Date: 2026-10-29

## Context

Ledgerline has five areas with their own rules: identity, wallets, the ledger,
payments and currency exchange. They must stay separate so each can change on
its own. Money must also stay consistent across them: a transfer takes money
from one wallet and gives it to another, and either both happen or neither does.

## Options

**A. Layered monolith.** One `controller`, one `service`, one `repository`
package for everything, as in most tutorials.
Simple to start, but nothing stops payment code from reading ledger tables
directly. After a year every class depends on every other, and nothing can be
changed or split out safely.

**B. Microservices.** One service and one database per area.
Each can be deployed and scaled alone. But a transfer then spans two services
and two databases, so it needs a saga: a chain of steps with "undo" steps if one
fails, plus retries and eventual consistency. That is the hardest way to keep
money correct, and it brings network failures, service discovery and many
pipelines. It pays off with several teams, not one developer.

**C. Modular monolith.** One application, one database, but each area is a
module with a public API and hidden internals, checked by a test.

## Decision

**C, with [Spring Modulith](https://spring.io/projects/spring-modulith).**

A transfer stays one database transaction, the most reliable way to keep money
correct. The boundaries are still real: the build fails when they're broken.
If one module ever needs to scale on its own, it can be split out along a
boundary that is already clean.

## How it works in the code

- Each direct sub-package of `com.ledgerline` is a module: `identity`, `wallets`,
  `ledger`, `payments`, `fx`.
- Types in a module's top package are its public API. Anything in a sub-package
  (for example `ledger.internal`) is hidden from other modules.
- `@ApplicationModule(allowedDependencies = ...)` in each `package-info.java`
  declares which modules it may use. `payments` may use `ledger` and `wallets`;
  `ledger` uses nobody.
- `ModularityTests.modulesRespectTheirBoundaries()` fails the build on a
  forbidden dependency, a use of another module's internals, or a dependency
  cycle. It also writes module diagrams to `backend/target/spring-modulith-docs`.
- Modules react to each other through events, stored in the `event_publication`
  table in the same transaction as the change (migration `V1`). An event is
  then never lost if the app stops before a listener has run.
