# Liberty Reckoner — Interview Questions and Model Answers

This guide covers the questions most likely to be asked in a full-stack, Java, system-design or project discussion. Do not memorise every sentence. Understand the decisions, explain the trade-offs, and be ready to open the relevant code.

## One-minute project explanation

### 1. Tell me about your project.

**Model answer:** Liberty Reckoner is an explainable custody-rights and release-assurance platform. It ingests verified prisoner, case, charge and custody data; calculates Section 479 BNSS action signals through a deterministic Java engine; creates role-owned tasks; writes alerts to a transactional outbox; and tracks the process through court order, bond and physical release. The backend is a Spring Boot modular monolith with PostgreSQL and Flyway. The frontend is React with hooks, Bootstrap and custom CSS. The complete system runs through Docker Compose.

### 2. What real problem does it solve?

It solves fragmented calculation and accountability. Knowing that a custody threshold was crossed is insufficient if the source data is uncertain, nobody is assigned to act, a court application is not prepared, or a release order does not become physical release. The product connects calculation, verification, notification and workflow.

### 3. What was your personal contribution?

Explain the parts you can defend: domain modelling, rule-engine design, transaction boundaries, REST contract, security, outbox, React UX, tests and Compose delivery. Give one difficult decision—for example, keeping legal rules deterministic and placing email behind an outbox port.

### 4. What is the most technically interesting part?

The eligibility engine and its consistency boundary. The engine merges overlapping custody periods, excludes only verified accused delay, chooses one-third or one-half thresholds, handles death/life exclusions and multiple active cases, and produces a versioned explanation. The service then stores the assessment, task, audit event and notification intent atomically.

## Architecture

### 5. Why did you choose a modular monolith instead of microservices?

Assessment, workflow creation, audit and notification intent share transactional invariants. A modular monolith provides one ACID transaction and simpler operations. Package and port boundaries still isolate responsibilities. Microservices would introduce distributed failure and eventual-consistency problems before scale or team ownership justified them.

### 6. How is the backend layered?

- `api`: controllers, request/response DTOs and error contract;
- `service`: use cases and transaction boundaries;
- `service/eligibility`: pure legal calculation;
- `domain`: JPA aggregates and enums;
- `repository`: persistence queries and locks;
- `security`: JWT cookie authentication;
- `notification`: delivery port, adapters and dispatcher;
- `config`: cross-cutting configuration and startup safety.

### 7. Is this clean architecture or hexagonal architecture?

It is a pragmatic layered modular monolith with a hexagonal boundary around external notification delivery. The pure engine is also infrastructure-independent. JPA entities are used directly inside the application layer, so it is not a strict textbook clean-architecture implementation. That trade-off reduces mapping overhead while preserving the most valuable boundaries.

### 8. Where are transaction boundaries placed?

At service methods that represent business actions: eligibility evaluation, workflow mutation, canonical integration and outbox state changes. Controllers do not start transactions. The dispatcher deliberately sends mail outside the database transaction, then records success or failure in another transaction.

### 9. Why does the scheduler live in a separate bean from `EligibilityService`?

Spring transaction annotations are proxy-based. A method calling another annotated method on `this` bypasses the proxy. A separate `EligibilityRefreshJob` calls the service proxy, so every case receives an independent transaction and one bad record cannot roll back the entire daily batch.

### 10. How would you draw the main request path?

Browser → Nginx → Spring Security → controller → service transaction → pure engine/repository → PostgreSQL → DTO → React. For alerts: assessment transaction → outbox row → scheduled dispatcher → notification port → logging or SMTP adapter.

## Domain and legal engine

### 11. Why is the rule engine deterministic rather than AI-based?

The output affects a person’s liberty and must be reproducible. The same verified input and rule version must produce the same result. AI may assist document extraction in a future ingestion stage, but it must not invent legal facts or make the eligibility determination.

### 12. How do you calculate credited custody days?

The engine filters to included and verified periods, normalises open periods to the calculation date, sorts intervals, merges overlapping or adjacent ranges, counts inclusive days, and subtracts verified accused-attributable delay. This prevents double-counting transfers or duplicated records.

### 13. How do you handle multiple charges?

The engine evaluates active charges, detects death/life possibilities, and uses the controlling finite maximum term for the calculation. Multiple offences or active cases produce a legal-review state because Section 479 contains a specific restriction. The software does not pretend that a simple arithmetic result resolves that legal question.

### 14. What if conviction history is missing?

The engine returns `DATA_INCOMPLETE`. It does not assume first-time-offender status, because that could select the lower one-third threshold without evidence.

### 15. Why use ceiling division for threshold days?

If the fraction of a maximum term is not a whole day, rounding down could signal eligibility early. Ceiling division ensures the complete statutory fraction has elapsed.

### 16. How do you prevent a new rule from changing historical results?

Every assessment stores its `ruleVersion`, calculated inputs, explanation and blockers. A future engine version creates a new assessment and marks the previous one historical. It does not overwrite the old explanation.

### 17. What is the difference between `ACTION_OVERDUE`, `BAIL_GRANTED` and `RELEASED`?

`ACTION_OVERDUE` is a decision-support signal. `BAIL_GRANTED` means an authorised user recorded the court outcome. `RELEASED` means physical release was verified after checking remaining cases or holds. Keeping these states separate prevents the dashboard from equating arithmetic with liberty.

## Database and consistency

### 18. Why PostgreSQL?

The project has strong relationships, uniqueness rules, transactional invariants, date-based reporting and audit history. PostgreSQL provides ACID transactions, constraints, indexes, row locking and mature operational tooling.

### 19. Why Flyway with `ddl-auto=validate`?

Flyway makes schema changes explicit, ordered and reviewable. Hibernate validation catches entity/schema drift without modifying production tables automatically. This makes deployments reproducible and rollback planning possible.

### 20. What indexes did you add and why?

Indexes cover prisoner search, prison joins, active cases, active charges, custody chronology, current assessment/status, task deadline, audit aggregate, integration event ID and outbox status/next-attempt time. They follow actual query paths instead of indexing every column.

### 21. How do you prevent two evaluations from creating two current assessments?

The service loads the `prisoner_case` with a pessimistic write lock. Competing evaluations for the same aggregate serialise. Inside that transaction, old current assessments are marked historical and the new one is inserted.

### 22. Where do you use optimistic locking?

Mutable `prisoner_case`, `workflow_task` and `outbound_notification` rows have JPA `@Version` fields. If two officials or workers update the same version, one update succeeds and the other becomes a controlled `409 CONCURRENT_UPDATE` rather than silently overwriting data.

### 23. What is idempotency and where is it implemented?

Idempotency means repeating the same source request has the same effect as processing it once. The integration API stores `(sourceSystem, sourceEventId)` with a SHA-256 payload hash. A replay with the same hash returns `ALREADY_PROCESSED`; reusing the ID with a different payload returns a conflict.

### 24. How would you handle database deadlocks?

Keep transactions short, acquire locks in a consistent aggregate order, index locking queries, monitor deadlock logs and retry only transactions known to be safe and idempotent. External network calls must remain outside locked transactions.

### 25. Is the audit table truly immutable?

It is append-only through application code, but database superusers can still change it. A government deployment should restrict database roles and export signed/immutable audit logs to an approved central store. I would not claim cryptographic immutability from this repository alone.

## Transactional outbox and notifications

### 26. Why not send email directly after calculating eligibility?

If email is sent before commit, a later rollback creates a false alert. If it is sent after commit without durable intent, a process crash can lose the alert. The outbox row is committed with the assessment, then a separate dispatcher retries delivery.

### 27. How does the dispatcher avoid duplicate workers claiming the same row?

It selects a bounded set of eligible rows with a pessimistic lock, marks them `PROCESSING` and commits that claim. JPA versions protect later updates. A unique event key prevents duplicate alert creation for the same assessment and recipient role.

### 28. What happens if the process crashes after claiming a message?

Rows stuck in `PROCESSING` for more than five minutes are recovered into `FAILED` with a new attempt time. They re-enter normal retry processing.

### 29. What happens after repeated provider failures?

The retry uses bounded exponential back-off. When the configured maximum attempt count is reached, the row enters `DEAD_LETTER`. Administrators and auditors can see it in the delivery ledger for manual intervention.

### 30. Is delivery exactly once?

No external email protocol generally guarantees exactly-once delivery across crashes. The design guarantees durable at-least-once delivery intent and minimises duplicates using event keys and state transitions. A provider idempotency key should be passed if the approved provider supports one.

### 31. How can another message provider be added?

Implement `NotificationDeliveryPort` and activate the adapter with configuration. The outbox and legal services do not need to change. That is the dependency-inversion benefit of the port.

## Spring Boot and API

### 32. Why use DTOs instead of returning entities?

DTOs prevent lazy-loading surprises, recursion, accidental exposure of password hashes or internal fields, and coupling the JSON contract to schema changes. They also provide request validation and stable error responses.

### 33. How are errors represented?

The global handler returns timestamp, HTTP status, stable error code, safe message, request path, request ID and field errors. Unexpected exceptions are logged server-side while the client receives no stack trace.

### 34. How is pagination implemented?

The custody registry accepts `page`, `size` and `query`. The service clamps page size to 100, creates a Spring Data `PageRequest`, applies deterministic name sorting and maps the `Page` into a generic `PageResponse`.

### 35. Why is the external integration API versioned but the browser API not fully versioned?

External adapters deploy independently and require a stable compatibility contract, so the canonical endpoint includes `/v1`. The React app and internal API deploy together and can evolve in lockstep. If independent clients appear, I would version or content-negotiate those contracts as well.

### 36. How would you document the API for external teams?

Provide the canonical payload examples in `docs/API.md`, stable error codes, idempotency semantics, authentication requirements, field definitions and version-deprecation policy. In a larger programme I would also publish a reviewed OpenAPI contract and contract tests.

## Security

### 37. How does authentication work?

The login endpoint verifies a BCrypt password and creates a signed JWT. The token is returned only in an HTTP-only, SameSite cookie, so frontend JavaScript cannot read it. A filter validates the cookie for later requests and loads the role into Spring Security.

### 38. Why do you still need CSRF protection with JWT?

Because the JWT is in a cookie, the browser attaches it automatically. A malicious site could cause a state-changing request unless a separate CSRF token is required. The frontend reads the non-HTTP-only CSRF cookie and mirrors it into the `X-XSRF-TOKEN` header.

### 39. How is authorisation enforced?

Broad route rules exist in `SecurityConfig`, while sensitive services/controllers use method-level `@PreAuthorize`. For example, only admins can ingest integration data and only admins/auditors can inspect notification delivery.

### 40. What security work remains for real deployment?

Government SSO, MFA policy, gateway rate limiting, managed keys, mTLS or approved network authentication, encryption controls, privacy/retention policy, VAPT, dependency scanning, central audit export and incident response. The local user table is a demonstration identity provider.

### 41. Why does the application fail fast in the `prod` profile?

It prevents common demonstration settings from reaching production: demo accounts, non-secure cookies, development signing key, localhost CORS and log-only notifications. Failing at startup is safer than serving traffic with a warning hidden in logs.

## React and frontend

### 42. Why use functional components and hooks?

They provide composable local state, effects and context without class lifecycle complexity. Route pages own server-query state, while reusable components remain mostly presentational.

### 43. Why did you not use Redux?

The only broadly shared state is the authenticated user and toast notifications. Case records are server state scoped to a route. Context plus local state is simpler. I would add a server-state cache such as TanStack Query before Redux if request caching and mutation invalidation became complex.

### 44. How does the frontend handle an expired session?

An Axios response interceptor detects `401` responses outside login and emits an application event. `AuthContext` clears the user, causing protected routes to redirect to login.

### 45. How did you design the UI for legal users?

The UI prioritises action ownership, deadline direction, verification state and explanation. It never relies only on colour. It separates portfolio overview, custody ledger, workflow and alert delivery. It also supports keyboard focus, responsive layouts, print output and reduced motion.

### 46. How is frontend code tested?

Vitest runs in jsdom with Testing Library. Tests verify human-readable legal status, compact status UI, deadline wording, enum formatting and identity fallbacks. The production Vite build then verifies module and route compilation.

### 47. What would you improve for a larger frontend?

Add route-level code splitting, a typed OpenAPI client, TanStack Query, form schemas, end-to-end Playwright tests, a formal component library, accessibility automation and real-time task updates where justified.

## DevOps and observability

### 48. What does Docker Compose contain?

PostgreSQL 16, a multi-stage Java 21 backend image and a multi-stage React/Nginx frontend image. Health checks order startup. Only Nginx publishes a host port; internal services communicate on the private Compose network.

### 49. Why use multi-stage Dockerfiles?

Build tools and source files stay in build stages. The final images contain only the runtime and compiled artefacts, reducing size and attack surface. The Java runtime also uses a non-root user.

### 50. What metrics would you monitor?

- assessment count by eligibility status;
- scheduled refresh failures;
- overdue workflow-task count and age;
- pending/failed/dead-letter notification count and oldest age;
- API error rate and latency;
- database pool saturation and slow queries;
- integration conflicts and event lag;
- time from threshold crossing to application, order and physical release.

### 51. How would you deploy with zero downtime?

Build immutable images, run backward-compatible Flyway migrations, start new instances, wait for readiness, shift traffic, then retire old instances. Destructive schema changes need expand-and-contract migrations. Scheduled work also needs leader election or a cluster-safe scheduler before multiple replicas run.

### 52. How would you scale this system?

First scale stateless API replicas and tune PostgreSQL. Add read replicas for analytics, partition large history tables, and use cluster-safe job coordination. Extract notification delivery only if volume or team ownership demands it. Avoid distributing the legal transaction prematurely.

## Failure and scenario questions

### 53. SMTP is down for six hours. What happens?

Legal assessments still commit because mail is decoupled. Outbox records retry with back-off and may enter dead-letter after the configured attempts. Operations can see failures, while metrics and alerts should notify support. After recovery, dead-letter records require an approved replay procedure.

### 54. Two officials complete the same task simultaneously. What happens?

Both may read the same version, but only one database update succeeds. The other receives a `409 CONCURRENT_UPDATE`, refreshes, and sees the completed state. This avoids duplicated next tasks and lost updates.

### 55. A source resends an event after timing out. What happens?

The integration key is found. If the payload hash matches, the existing result is returned as `ALREADY_PROCESSED`. If it differs, the API returns a conflict because silently accepting a changed event under the same ID would break auditability.

### 56. A custody period is later corrected. How is history preserved?

The canonical case record is updated by an authorised, audited integration event and evaluated again. The old assessment becomes historical with its original rule version and values; the new assessment becomes current. Production should also retain source-system correction provenance.

### 57. The rules change after a court judgment. What do you do?

Create a new rule version, add legal-regression fixtures, obtain approval, deploy it, and re-evaluate active cases. Never mutate historical assessment rows. Compare changed outcomes in shadow mode before activating workflow effects.

### 58. A person has crossed the threshold but another active case exists. What does the system show?

It shows `LEGAL_REVIEW`, explains the multiple-case blocker and assigns DLSA review. It does not declare release. The maximum-period safeguard and other legal remedies remain visible for human action.

## Honest limitations

### 59. What are the current limitations?

- The sentence catalogue is representative demonstration data, not an approved national legal master.
- Government SSO and real ICJS/e-Prisons adapters are not bundled.
- The SMTP adapter requires an approved provider and production credentials.
- Audit immutability depends on deployment-level database and log controls.
- Cluster-safe scheduling is required before multiple scheduler replicas run.
- Full browser end-to-end and accessibility certification remain production assurance tasks.

### 60. What would you build next?

My next priorities would be an approved offence/sentence master-data workflow, government identity federation, signed adapter authentication, dead-letter replay with four-eyes approval, Playwright end-to-end tests, accessibility automation and outcome-time analytics that measure threshold-to-release delay.

## Final interview advice

When presenting the project:

1. Start with the human and operational problem.
2. Explain why a calculator alone is unsafe.
3. Draw the modular monolith and outbox flow.
4. Walk through one eligibility test.
5. Discuss one failure scenario and one security trade-off.
6. Be explicit about legal and deployment limitations.
7. Finish with measurable impact: fewer missed thresholds and shorter time from eligibility to action and release.
