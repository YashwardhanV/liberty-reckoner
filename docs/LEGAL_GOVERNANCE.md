# Legal-rule governance

Liberty Reckoner contains an implementation template, not an administratively approved national sentence catalogue. The seeded charge entries are explicitly marked as demonstration data requiring legal validation.

## Required governance body

A production rule release should be signed off by representatives of the appropriate judicial/eCommittee authority, Department of Justice, MHA/NCRB, NALSA, State prison administration and qualified criminal-law reviewers.

## Change-control requirements

Every rule or offence-catalogue change must include:

1. Primary legal authority and effective date.
2. Applicability to cases registered before and after the effective date.
3. Finite maximum imprisonment in an approved canonical representation.
4. Death and life-imprisonment flags.
5. Special-law interaction and known interpretive uncertainty.
6. Positive, negative, boundary and regression test cases.
7. Reviewer identity and approval timestamp.
8. Migration and rollback plan.
9. Notice to operational users when an existing assessment changes.

## Human-control boundaries

- AI may extract text from scanned records, but extracted charges and dates remain unverified until confirmed.
- No prediction of guilt, flight risk, community danger or judicial outcome is permitted.
- No automatic release instruction is produced.
- Multiple offences, multiple cases, missing legal terms, disputed custody and exclusions go to a human queue.
- Application packets remain drafts until signed by the responsible official.
- Physical release requires verification of every active case, warrant and other lawful hold.

## Quality metrics

The most important safety metric is a verified missed threshold crossing. Teams should additionally monitor calculation reversals, missing-source rates, time from threshold to application, application-to-hearing time, order-to-release time and bail-granted persons still detained.
