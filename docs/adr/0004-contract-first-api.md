# 0004. Contract-first API, checked on both sides

- Status: accepted
- Date: 2026-10-30

## Context

The backend (Java) and the frontend (TypeScript) must agree on every request
and response: field names, which fields are required, enum values, error
shapes. If they drift, the bug shows up in the browser at runtime, usually as a
wrong number on a screen. In a money app that is the worst place to find it.

We want one source of truth, types on both sides that come from it, and a build
that fails when either side stops matching it.

## Options

**A. Code-first.** Write the Spring controllers, let springdoc generate the
OpenAPI document from them, and generate the frontend client from that.
Least work at first. But the contract is whatever the Java code happens to
produce: an innocent rename in a Java record silently changes the API, and the
frontend can only start once the backend exists.

**B. Contract-first, generate the Java too.** Write `api/openapi.yaml`, generate
Spring interfaces and model classes with openapi-generator, implement the
interfaces.
Both sides are generated, so neither can drift. But the generated Java models
are mutable classes with getters and setters, not records; they use Spring's
deprecated `@Nullable` (Spring 7 moved to JSpecify), which breaks our
`-Werror` build; and they aren't null-marked, so NullAway can't check the code
that uses them. We would spend time fighting the generator instead of writing
the domain.

**C. Contract-first, hand-written Java checked against the contract.** Write
`api/openapi.yaml` first. The frontend generates its types, client, zod schemas
and TanStack Query hooks from it. The backend writes its JSON shapes as small
Java records, and an API test sends real requests and validates every response
against `openapi.yaml` (Atlassian's swagger-request-validator).

## Decision

Option C.

- `api/openapi.yaml` is the contract. A change to the API starts there, and the
  pull request shows the API change before any code.
- **Frontend:** `npm run api:generate` (hey-api) produces the types and client in
  `src/shared/api/generated`. The SDK also validates every response with the
  generated zod schemas at runtime, so a backend that drifts fails loudly
  instead of rendering bad data. The generated code is committed: it is
  deterministic, and the diff shows what changed for the frontend.
- **Backend:** request and response records live next to the controller
  (`WalletResponses`). `WalletApiTests` checks each response against the
  contract, so a field the contract doesn't have, a missing required field, a
  wrong status code or a wrong content type fails `./mvnw verify`.

## Conventions in the contract

- **Money is a decimal string** (`"1250.00"`) plus a currency, never a JSON
  number. JSON numbers are parsed as floating point in JavaScript, and 0.1 + 0.2
  is not 0.3. The backend uses `BigDecimal`; the frontend formats the string
  with `Intl.NumberFormat` without converting it to a number.
- **Errors are RFC 9457 problem details** (`application/problem+json`) with a
  `detail` written for the user ("You already have a USD wallet."), so the
  frontend can show it as is.
- **Lists are wrapped** (`{ "items": [...] }`), so paging fields can be added
  later without breaking clients.
- **URLs are versioned** (`/api/v1`). A breaking change gets `/api/v2`; additions
  don't.

## Consequences

- Two places to edit for an API change (the YAML and the Java records), but the
  test makes forgetting the second one impossible to merge.
- The contract test covers the responses the tests produce. An endpoint without
  a test isn't checked, so every endpoint gets one.
- If the generator's Java output improves (records, JSpecify), option B becomes
  cheap to switch to: the contract doesn't change.
