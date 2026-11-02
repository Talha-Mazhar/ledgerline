# 0009. Events through a transactional outbox to Kafka

- Status: accepted
- Date: 2026-11-01

## Context

A payment that completes has consequences outside the payments module: an email and an in-app
notification, an audit entry, later perhaps analytics. Calling those from the payment code couples
modules and makes a slow email server slow down payments. Sending a Kafka message directly from the
transaction has the dual-write problem: the database commits and Kafka is down (the event is lost),
or Kafka gets the message and the transaction rolls back (an event about something that never
happened).

## Decision

- Modules publish domain events (`PaymentCompleted`, `PaymentHeld`, `UserRegistered`, …) with
  Spring Modulith. Modulith's **event publication registry** writes each event to a table in the
  *same transaction* as the payment: the transactional outbox. Events annotated `@Externalized`
  are sent to Kafka after the commit, keyed by payment or user id so one payment's events stay in
  order on one partition.
- If Kafka is down the publication stays incomplete in the table. A scheduled job
  (`OutboxResubmitter`, guarded by ShedLock so one replica runs it) resubmits incomplete
  publications older than a minute, and the app resubmits them at start-up. A test stops Kafka,
  makes a payment, starts Kafka and checks the notification still arrives.
- Messages are JSON strings; consumers parse them with Jackson into their own small records, so no
  module shares Java classes over Kafka.
- Consumers retry with back-off on retry topics (1 s, 2 s, 4 s) without blocking other messages;
  after four failures a message goes to `<topic>-dlt`.
- Consumers are idempotent: a notification has a unique `(event_id, user_id)`, so a message
  delivered twice is stored once. Kafka gives at-least-once delivery; the database makes it
  effectively once.
- Inside the process, the audit log listens with `@ApplicationModuleListener` (also backed by the
  registry), not through Kafka.

## Consequences

Notifications arrive a moment after the payment, not in the same request; the frontend polls the
inbox every 30 seconds. Running Kafka is a real operational cost; in production it is a managed
service (see 0012).
