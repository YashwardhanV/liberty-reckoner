# Security policy

## Reporting a vulnerability

Do not place prisoner information, credentials or exploit details in a public issue. Report vulnerabilities through the deployment organisation's approved security-response channel. Include the affected version, reproducible impact and a non-sensitive proof of concept.

## Security controls in this repository

- Role-based Spring Security authorisation
- BCrypt password hashing
- Short-lived signed sessions in HTTP-only, SameSite cookies
- CSRF token protection for state-changing requests
- Explicit CORS origin allowlist
- Generic API errors with per-request identifiers
- Flyway-managed schema and database constraints
- Idempotent integration events with payload hashes
- Transactional notification outbox with bounded retries and dead-letter state
- Optimistic versions on mutable legal-workflow aggregates
- Production-profile startup guardrails for demonstration settings
- Prometheus-compatible operational metrics
- Audit events for calculations, workflow outcomes and synchronisation
- Nginx content-security, frame, referrer and permission headers
- Private database/backend Compose networking
- Non-root Java runtime container
- No frontend persistence of bearer tokens

## Production controls outside the repository

The reference code does not replace the deployment authority's controls. Production requires TLS, approved secret management, gateway rate limits, central SSO or identity federation, network segmentation, encryption at rest, backup testing, security monitoring, privileged-access review, retention enforcement and independent penetration testing.

Set `LIBERTY_RECKONER_SEED_DEMO_DATA=false`, rotate every demonstration credential, set `LIBERTY_RECKONER_SECURE_COOKIE=true`, use a random JWT secret of at least 32 bytes, restrict `LIBERTY_RECKONER_CORS_ALLOWED_ORIGINS` to the exact deployed origin and configure an approved notification transport. The `prod` profile enforces these baseline checks at startup.

## Data handling

Prisoner records are highly sensitive. Do not expose the application directly to the public internet, include personal data in email notifications, or export production data into development/test environments. Use synthetic or formally anonymised fixtures for engineering and demonstrations.
