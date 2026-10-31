# 0007. Sign-in with server sessions and CSRF tokens, not JWTs

- Status: accepted
- Date: 2026-10-31
- Replaces the demo user of [0005](0005-wallets-design.md), section 3.

## Context

Ledgerline's frontend is served from the same origin as its API (nginx forwards `/api` to the
backend; Vite does the same in development). Users sign in with email and password.

## Options

**A. JWT in local storage**, sent in an `Authorization` header. No CSRF problem, but any script
that runs on the page (an XSS bug, a compromised npm package) can read the token and use it
elsewhere until it expires. Signing out can't revoke it without a server-side list, which brings
back the state JWTs were meant to avoid.

**B. Server session in an `HttpOnly` cookie** (chosen). Scripts can't read the cookie, sign-out
ends the session on the server at once, and Spring Security supports it out of the box. The cost
is CSRF: the browser attaches the cookie to requests other sites trigger, so every state-changing
request must also prove it came from Ledgerline's pages.

## Decision

- `POST /auth/sign-in` checks the password (bcrypt via a delegating encoder, so the algorithm can
  be upgraded later) and starts a session the same way Spring's form login does: a new session id
  (against session fixation), a new CSRF token, and the user stored in the session.
- **CSRF: the cookie-to-header pattern.** The backend sets an `XSRF-TOKEN` cookie that pages can
  read; the frontend copies it into an `X-XSRF-TOKEN` header on every POST. Another site can make
  the browser send cookies, but can't read them, so it can't produce the header.
- Wrong email and wrong password give the same answer, and an unknown email still runs a bcrypt
  check, so neither the message nor the response time tells which accounts exist.
- 401 and 403 are problem details like every other error. The frontend treats any 401 as "the
  session ended" and returns to the sign-in page.
- The rest of the code asks `CurrentUser` for the user's id, as before; only its implementation
  changed.

## Not done yet

Rate limiting of sign-in attempts, email verification and password reset. A production deployment
would add them, plus `Secure` cookies behind HTTPS.
