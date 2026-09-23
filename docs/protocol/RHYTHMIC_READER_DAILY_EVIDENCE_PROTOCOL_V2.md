# Rhythmic Reader Daily Evidence Protocol V2

## Purpose and ownership

Protocol V2 exposes a read-only numerical snapshot of credible reading for one device-local calendar date. Rhythmic Reader is the evidence authority. Rhythmic Routine remains the policy authority and decides how this evidence affects its own behavior.

V2 is additive. It does not change the recovery-session lifecycle, Intent contract, provider authority, or row semantics defined by [Protocol V1](RHYTHMIC_READER_PROTOCOL_V1.md).

## Security

- Authority: `com.terinit.rhythmicreader.evidence`
- Read URI: `content://com.terinit.rhythmicreader.evidence/daily/{dateKey}`
- Exported provider read and write permissions: `com.terinit.rhythmicreader.permission.RECOVERY`
- The permission is declared by Rhythmic Reader with Android `signature` protection. Routine must hold the same-signature permission.
- The provider is read-only. `insert()`, `update()`, and `delete()` are unsupported.

## Exact-date query

`dateKey` must be a valid ISO local date in `yyyy-MM-dd` form. The key is interpreted using the device's local calendar date, not a UTC day boundary. Only one `/daily/{dateKey}` row query is supported; directory listings, arbitrary selections, unsupported projections, malformed dates, and extra path segments are rejected safely.

A matching query returns one row when that date has a ledger row. A valid date without a row returns an empty cursor. The provider does not create or modify evidence while servicing a query.

## Row schema

| Column | Type | Meaning |
|---|---|---|
| `protocolVersion` | Integer | Always `2` |
| `dateKey` | String | Requested local date key |
| `verifiedActiveSeconds` | Long | Credible active reading time accumulated for that date |
| `qualifiedPages` | Integer | Distinct pages that passed the existing dwell qualification on that date |
| `updatedAtEpochMs` | Long | Last evidence update timestamp in epoch milliseconds |

No book name, filename, document URI, file path, page text, reading position, annotation, or library metadata is exposed.

## Evidence semantics

- Verified time uses the same Reader-controlled credibility gates as recovery reading: foreground application, interactive screen, successfully loaded document, and a readable Reader surface.
- One shared monotonic reading tracker supplies daily and active recovery-session projections. A recovery session does not start another timer.
- Pages are qualified by the existing dwell and rapid-flip engine. The daily unique key is `(dateKey, bookId, pageIndex)`, so a page contributes at most once on a local date and may qualify again on a later date.
- Time deltas that span local midnight are split between the corresponding date ledgers. Previous-date rows remain unchanged by subsequent-day activity.
- Protocol V2 contains evidence only. It does not decide unlocks, restrictions, cooldowns, or access policy.
