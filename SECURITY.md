# Security

Ledgerline is a portfolio project: it moves no real money, and its demo accounts and passwords
are public. Please still report anything you find.

## Reporting

Open a [private security advisory](https://github.com/Talha-Mazhar/ledgerline/security/advisories/new)
on GitHub rather than a public issue. I aim to answer within a week.

## What is in place

- Sessions in `HttpOnly`, `SameSite=Lax` cookies (`Secure` behind HTTPS) and CSRF tokens on every
  state-changing request ([ADR 0007](docs/adr/0007-sessions-and-csrf.md)).
- Role checks on URLs and on methods; staff decisions in an append-only audit log.
- Uploaded documents checked by content and size, stored outside the database, served only to
  staff ([ADR 0010](docs/adr/0010-identity-documents-and-tiers.md)).
- Containers run as non-root users with read-only file systems; network policies in Kubernetes;
  metrics and health on a port the ingress never exposes.
- CodeQL, Trivy (dependencies, images, Dockerfiles, Helm chart), dependency review on pull
  requests, and Dependabot.
