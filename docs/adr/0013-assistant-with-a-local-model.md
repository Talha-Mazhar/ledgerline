# 0013. "Where did my money go?" with a local model and read-only tools

- Status: accepted
- Date: 2026-11-01

## Context

Customers ask questions a fixed report doesn't answer: "how much did I spend on transfers last
month?", "why is my balance lower than I thought?". A language model can answer them, but a
customer's transactions are personal data, and a model must never be able to move money or see
another customer.

## Decision

- **Spring AI** with **Ollama** running a small open model (Llama 3.2, 3B) on Ledgerline's own
  servers. No transaction leaves the infrastructure; there is no third-party API key.
- The model gets **tools, not data**: `balances`, `activity(from, to)` and `summary(from, to)` (money in and out per currency),
  implemented with the same read services the API uses. Each tool reads only the signed-in
  customer's wallets; the customer id comes from the session, never from the model.
- No tool writes. The system prompt tells the model to answer only from tool results and to say
  when it doesn't know.
- If the model server is down or slow, the endpoint answers 503 with a clear message; nothing
  else in the app depends on it.

## Consequences

Answers from a 3B model are short and sometimes clumsy; a larger model is a configuration change.
The model server needs about 2 GB of memory, so it is an optional Compose profile (`ai`) and
optional in the Helm chart.
