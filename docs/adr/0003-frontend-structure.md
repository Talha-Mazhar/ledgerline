# 0003. Feature-based frontend structure

- Status: accepted
- Date: 2026-10-29

## Context

The web app will grow to many screens: overview, wallets, transfers, exchange,
history, settings. Code needs a home that stays obvious as it grows, and server
data needs one consistent way to be fetched and cached.

## Options

**Folder layout**

- **By file type** (`components/`, `hooks/`, `pages/`, `services/`): familiar,
  but one feature is spread over four folders, and those folders fill up with
  unrelated files.
- **By feature** (`features/transfers/` holds its pages, components and API
  hooks): what changes together lives together, and a feature can be deleted by
  deleting one folder.
- **Feature-Sliced Design** (strict layers: app, pages, widgets, features,
  entities, shared): very scalable, but its seven layers and rules are more than
  this app needs today.

**Server state**

- **Redux Toolkit / RTK Query**: a global store; RTK Query adds caching. Useful
  when lots of client-only state must be shared.
- **TanStack Query**: caching, refetching, retries, loading and error states for
  server data, with no store to maintain. Our generated API client produces
  TanStack Query helpers directly.

## Decision

**By feature, plus TanStack Query for everything that comes from the API.**

```
src/app/       shell: providers, routes, theme, layout, error pages
src/features/  one folder per feature
src/shared/    used by several features: api (generated), config, ui
src/test/      test setup and helpers
```

Rules: features may import from `shared/` and `app/`, never from each other;
shared code moves to `shared/`. Route paths live in `shared/config/paths.ts`.
Server data is never copied into component state or a global store.

## Consequences

- Adding a page means one folder under `features/`, a route in
  `app/routes.tsx`, and a menu entry in `app/layout/navigation.ts`.
- Tests render the real routes through `renderRoute()` and find elements by role
  and label, so they check what a user sees rather than implementation details.
