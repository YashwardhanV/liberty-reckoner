# Liberty Reckoner API

Base path: `/api`

Authentication uses an HTTP-only `LIBERTY_RECKONER_SESSION` cookie. Before a state-changing request, call `GET /auth/csrf`; Axios is configured to return the `XSRF-TOKEN` value in the `X-XSRF-TOKEN` header.

## User and dashboard endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/auth/csrf` | Initialise CSRF protection |
| `POST` | `/auth/login` | Authenticate and establish a secure session |
| `POST` | `/auth/logout` | Clear the session |
| `GET` | `/auth/me` | Return the current role and profile |
| `GET` | `/dashboard/summary` | Operational metrics, urgent cases and occupancy |
| `GET` | `/prisoners?query=&page=&size=` | Paginated custody registry |
| `GET` | `/prisoners/{id}` | Complete person–case workspace |
| `POST` | `/cases/{id}/evaluate` | Re-run and persist an eligibility assessment |
| `GET` | `/cases/{id}/application-packet` | Generate a Section 479(3) draft |
| `GET` | `/workflows/queue` | Open and in-progress hand-offs |
| `POST` | `/workflows/{id}/start` | Take ownership of an open task |
| `POST` | `/workflows/{id}/complete` | Record outcome and create the next hand-off |
| `GET` | `/notifications/outbox?status=` | Recent alert delivery records; admin/auditor only |
| `GET` | `/rules` | Explainable rule catalogue |

Notification status may be `PENDING`, `PROCESSING`, `DELIVERED`, `FAILED` or `DEAD_LETTER`. The endpoint returns at most the 100 most recent records and may be filtered by one status.

## Canonical justice-record integration

`PUT /integrations/v1/justice-records` is restricted to administrators and intended for approved machine adapters. It accepts a full person–case snapshot.

```json
{
  "sourceSystem": "e-Prisons-DELHI",
  "sourceEventId": "EP-2026-00001883-v4",
  "prisonCode": "DL-TIH-01",
  "prisonNumber": "UTP-2026-0912",
  "fullName": "Demonstration Person",
  "gender": "MALE",
  "dateOfBirth": "1994-05-18",
  "nationality": "Indian",
  "preferredLanguage": "Hindi",
  "previousConvictions": 0,
  "convictionHistoryVerified": true,
  "cnrNumber": "DLND01-001883-2026",
  "firNumber": "912/2026",
  "policeStation": "New Delhi Police Station",
  "courtName": "District & Sessions Court, New Delhi",
  "district": "New Delhi",
  "state": "Delhi",
  "stage": "TRIAL",
  "chargeSheetDate": "2026-06-15",
  "nextHearingDate": "2026-08-24",
  "accusedDelayDays": 0,
  "accusedDelayVerified": true,
  "charges": [
    {
      "actName": "BNS",
      "sectionCode": "303(2)",
      "description": "Theft",
      "maximumTermDays": 1095,
      "maximumTermLabel": "3 years",
      "deathPunishmentPossible": false,
      "lifeImprisonmentPossible": false,
      "effectiveFrom": "2024-07-01",
      "legalSource": "Approved catalogue release 2026.2"
    }
  ],
  "custodyPeriods": [
    {
      "startDate": "2025-08-01",
      "endDate": null,
      "included": true,
      "exclusionReason": null,
      "sourceSystem": "e-Prisons remand ledger",
      "verified": true
    }
  ]
}
```

Possible integration outcomes are `CREATED`, `UPDATED` and `ALREADY_PROCESSED`. Reusing a source event ID with a different payload returns HTTP 409.

All API errors contain a stable code, safe message, request ID and optional field-error map. Sensitive stack traces are never returned.
