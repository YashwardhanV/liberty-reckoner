# Liberty Reckoner architecture

## Operating model

```text
ICJS / e-Prisons / CIS adapter
             │
             ▼
 Canonical integration API ── idempotency ledger
             │
             ▼
 Person–case reconciliation ── custody ledger ── charge catalogue
             │
             ▼
 Deterministic eligibility engine
             │
      ┌──────┼────────────────┐
      ▼      ▼                ▼
Explainable  Human task   Notification outbox
 result      workflow          │
      │      │                 ▼
      └──────┴────────── Retrying delivery adapter
             ▼
 Application → filing → hearing → bond → release
             │
             ▼
       Immutable audit events
```

The platform stores a person independently from a legal case. `prisoner_case` is the join that owns case-specific custody periods, charges, assessments and workflow tasks. This supports co-accused matters and multiple cases without incorrectly treating an FIR as a person identifier.

## Main data relationships

| Aggregate | Important relationships |
|---|---|
| Prison | Has many prisoners; capacity is operational metadata |
| Prisoner | Belongs to a current prison; has many prisoner-case links |
| Legal case | Can relate to multiple prisoners through prisoner-case links |
| Prisoner case | Owns active charges, custody periods, assessment history and workflow tasks |
| Legal assessment | Append-only calculation history; one row is marked current |
| Workflow task | Role-specific owner, deadline, state and recorded outcome |
| Integration event | Unique source-system/event ID and payload hash for idempotency |
| Outbound notification | Durable role-specific delivery intent, attempts, retry state and provider outcome |
| Audit event | Actor, action, timestamp and JSON detail for material operations |

## Consistency and asynchronous delivery

Eligibility evaluation locks one prisoner-case aggregate, retires the old current assessment, and writes the new assessment, first workflow action, audit event and notification intents in one PostgreSQL transaction. Mutable case, task and outbox records also carry optimistic versions so competing officials or workers cannot silently overwrite each other.

The notification dispatcher uses a transactional outbox. It claims a small row-locked batch, commits the claim, calls the configured provider outside the claim transaction, then records delivery or schedules an exponential retry. Interrupted `PROCESSING` claims are recovered after five minutes; exhausted retries enter `DEAD_LETTER` for manual review. The delivery provider is a hexagonal port with demonstration logging and SMTP adapters.

The scheduled eligibility refresh is deliberately a separate component. It calls the transactional evaluation service once per case, isolating record-level failures instead of processing the entire prison population in a single transaction.

## Eligibility calculation

`EligibilityEngine` is a pure deterministic component. Its inputs are already-normalised legal facts; it does not query external systems and does not use AI.

1. Merge overlapping verified custody periods.
2. Subtract only verified accused-caused delay days.
3. Stop when conviction history, delay exclusions, custody periods or charge terms are not verified.
4. Route death/life-punishable charges outside the Section 479 fraction workflow.
5. Select one-third for a person never previously convicted; otherwise select one-half.
6. Prioritise the maximum-period safeguard.
7. Route multiple offences or active cases to human legal review.
8. Classify the remaining result as not due, due within 90 days or action overdue.

Every stored assessment contains the rule version, counted days, excluded days, threshold, projected date, explanation and blockers.

## Integration boundary

The canonical integration endpoint accepts a complete person–case snapshot from an approved adapter. It is idempotent on `sourceSystem + sourceEventId` and rejects reuse of an event ID with a changed payload.

Production adapters should:

- Resolve e-Prisons prison IDs to Liberty Reckoner prison codes through an approved master-data table.
- Preserve CNR, FIR and prison identifiers rather than relying on names.
- Transmit current charge-framing information rather than only initial FIR sections.
- Send source timestamps and verified custody intervals.
- Retry with the same source event ID.
- Reconcile response outcomes and quarantine rejected records.

## Deployment

The reference deployment uses Nginx as the only exposed service. It serves the compiled React application and proxies `/api` to the Spring Boot service. The application service and PostgreSQL remain private. In production this layer should sit behind the approved government gateway, TLS termination, central identity controls, logging and rate limiting. The `prod` profile fails startup when demonstration seeding, insecure cookies, local CORS, the development signing key or the log-only notification adapter is present.
