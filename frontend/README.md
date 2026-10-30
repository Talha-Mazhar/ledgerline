# Ledgerline web app

React 19, TypeScript (strict), Vite, MUI, TanStack Query and React Router.

```bash
npm run dev          # http://localhost:5173, /api is proxied to the backend on :8080
npm run check        # typecheck, lint, format check, tests: what CI runs
npm run build        # production build in dist/
npm run api:generate # regenerate src/shared/api/generated from ../api/openapi.yaml
```

## Structure

```
src/
├── app/           the shell: providers, routes, theme, layout, error pages
├── features/      one folder per feature (overview, wallets, transfers, ...)
├── shared/        code used by several features
│   ├── api/       generated from the OpenAPI contract, never edited by hand
│   ├── config/    route paths and other constants
│   └── ui/        reusable components built on MUI
└── test/          test setup and helpers
```

A feature may import from `shared/` and `app/`; features never import from each other. If
two features need the same thing, it moves to `shared/`.

## Rules

- No `any`. Untrusted data (API responses, `JSON.parse`, thrown errors) is `unknown` and is
  narrowed before use. The linter enforces this.
- Server data lives in TanStack Query, not in component state or a global store.
- Every route path comes from `shared/config/paths.ts`.
- Tests render the real routes with `renderRoute()` and query by role and label, the way a
  user finds things.
