# 0010. Identity documents in S3, tiers and staff review

- Status: accepted
- Date: 2026-11-01

## Context

Higher limits need a verified identity (KYC). Customers upload a passport or ID card; staff look at
it and decide. Documents are personal data: they must not sit in the database, be served to anyone
but staff, or be trusted because of a file name.

## Decision

- Documents go to **object storage over the S3 API**: RustFS (S3-compatible, open source) locally and in the demo cluster, Amazon
  S3 in production, with credentials from the pod's role there (no keys in configuration). The
  `documents` module hides which one behind `DocumentStore`.
- The upload is checked by its **content**: the first bytes must be a PDF, JPEG or PNG signature,
  and it must be at most 5 MB. The declared content type and name are not trusted.
- A customer is `BASIC` until staff approve a document, then `VERIFIED`. Limits per tier are
  configuration (`ledgerline.risk.*`), read by the risk rules of 0008.
- Only `SUPPORT` and `ADMIN` can list the review queue or download a document; method security
  (`@PreAuthorize`) and the URL rules both check it. Every decision goes to the append-only audit
  log with who made it.
- A rejection carries a reason the customer sees, and they can upload again.

## Not done

Document expiry dates, liveness checks and automatic document verification, which a real bank
would buy from a KYC provider.
