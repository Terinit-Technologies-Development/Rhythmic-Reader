# SW-2026-005 C3 Reader handoff

**Baseline:** Reader source handoff `4a45e2075fb920e379a9acc78eabf858bfc0eb45`;
installed artifact `1.3.1` / code `4`; applicationId
`com.terinit.rhythmicreader`.

**Status:** source correction implemented for Root review. Root owns all shell,
test, build, signing, git, and phone verification. No acceptance claim is made
here.

## 1. Observed failure

A CD4 `RestorativeChoiceCard` entry opened the Reader, which settled on the
generic Library. A read-only copy of the Reader Room database contained no new
`recovery_sessions` row for the requested CD4 binding; the latest row remained
the pre-existing 30 Sep `ACTIVE` record. No record was changed, deleted, or
seeded.

## 2. Root cause

The Routine binding is the stable opaque string:

```text
gate-ad-20261103-0800-sw005-qa-cycle-4-o4-reader
```

The installed Reader required a canonical UUID:

```text
RecoveryEntryActivity.onCreate
  -> RecoveryRequestParser.fromIntent
  -> RecoveryRequest.validate
  -> isValidUuid
```

`UUID.fromString()` failed, the parser returned `null`, and
`RecoveryEntryActivity` finished before `acceptExternalRequest`. That explains
the absent row and the absence of new reading progress. The pre-existing 30 Sep
`ACTIVE` row is a different binding and was preserved.

The provider/query path did **not** require a UUID; it already performed an
exact string lookup. The effective format gate existed only in
`RecoveryRequest.validate()`, repeated through `request.isValid` in the
repository.

## 3. FR-09 bound entry gap

Even a successfully accepted request did not route to recovery UI.
`RecoveryEntryActivity` launched `MainActivity` with the session ID, but
`MainActivity` only restored the coordinator session. `ReaderApp` always
initialized to `Screen.Library` and never reacted to the entry. Focus and the
Reader recovery badge were reachable only by manual navigation or opening a
book.

The source correction emits a one-shot entry only after an explicit
`restoreSession(sessionId)` succeeds. `ReaderApp` opens the existing Focus
screen unless a Reader surface is already visible. `onCreate` and
`onNewIntent` are both covered. An ordinary launch does not route because the
pre-existing 30 Sep `ACTIVE` row is attached.

## 4. Contract decision

This is an additive V1 clarification, not a wire-version change:

- `recovery.session_id` remains a `String` with protocol version `1`.
- Canonical UUID acceptance and behavior are unchanged.
- Stable opaque IDs are accepted with bounds: `1–128` ASCII characters, first
  `[A-Za-z0-9]`, remaining `[A-Za-z0-9._:-]`.
- Opaque IDs are persisted and queried exactly as supplied; no trim,
  normalization, or rewrite.
- Signature trust, immutable metadata/replay, unknown-is-not-zero, timing,
  pages, provider URI/columns, schema, and existing records are unchanged.

**Compatibility consequence:** older UUID-only Reader builds reject current
Routine opaque IDs even though the protocol version is `1`. The coordinated
minimum Reader build is code `5` (`versionName 1.3.1`).

## 5. Source mapping

| Concern | Path | Change |
|---|---|---|
| ID predicate | `app/src/main/java/com/terinit/rhythmicreader/integration/rhythmic/RecoveryRequest.kt` | Keep `isValidUuid`; add `isValidSessionId`; `validate` uses it |
| Entry event | `app/src/main/java/com/terinit/rhythmicreader/app/MainActivity.kt` | Emit one-shot request only after exact restore succeeds |
| Entry route | `app/src/main/java/com/terinit/rhythmicreader/app/ReaderApp.kt` | Open Focus unless already on Reader |
| Version | `app/build.gradle.kts` | `versionCode 5`; `versionName 1.3.1` |

Unchanged production surfaces: Manifest/permissions, signing, Room schema,
DAO/entity/provider contract, repository acceptance and replay comparison,
qualification/timing engine, Focus/RecoveryCard UI, and existing records.

## 6. Focused checks — NOT RUN by source author

- Exact opaque ID parses and is accepted through the real Room repository.
- Status provider returns one row for the exact opaque ID, with protocol `1`,
  `ACTIVE`, and zero initial progress.
- Exact replay returns the same row and preserves active time and pages;
  conflicting replay returns `null` without mutation.
- Canonical UUID behavior remains unchanged.
- Empty, over-length, whitespace, slash, control, and leading-punctuation IDs
  are rejected.
- Unknown opaque ID returns a zero-row cursor, not null and not a zeroed row.
- Ordinary launch with the old Sep30 `ACTIVE` row does not route to Focus.
- A real first entry and a repeat entry are Root-owned phone checks.

No physical, signing, or acceptance result is claimed.

## Root verification — 3 November 2026
Root independently ran the complete unit suite and debug assembly: 119 tests, zero failures/errors/skips; BUILD SUCCESSFUL in 2m39s. Product source hashes remained unchanged during checks. Evidence: c3-reader-handoff-precommit-r1.json and matching build log. Independent review, original-signer qualification and physical entry/retry tests remain pending.

