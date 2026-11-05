# SW-2026-005 C3 Reader handoff

**Baseline:** Reader source handoff `4a45e2075fb920e379a9acc78eabf858bfc0eb45`;
current worktree `6d4d7457baa134d25159be17d1cbca7eb3d0d0f1`. The
opaque-ID/bound-entry correction was accepted as code `5` and installed by Root
with the existing recovery row preserved. This change targets code `6`;
applicationId `com.terinit.rhythmicreader`.

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

## 4. Foreground-resume visibility defect (code 6)

Root reproduction receipt: `c3-reader-resume-visibility-reproduction.json`.
With the QA PDF visible and reading in the normal foreground, the V2 ledger
recorded 1108 seconds at `1793724549192`. Returning from background to Reader,
including multiple returns through 19:01, left the persisted 1108 seconds
unchanged for over ten minutes while the PDF remained visible. Closing Reader
back to Library and normally reopening the same QA book at 19:03 restored
credit to 1188 seconds by `1793725476778`.

The cause was in `ReaderScreen`'s `DisposableEffect(lifecycleOwner)` observer.
It captured the initial `viewerState` (`null`) and initial `uiState`. On
`ON_RESUME` it therefore called `onReaderVisibleChanged(false)`. The separate
`LaunchedEffect` keys had not changed, so visibility was never restored. The
daily V2 and qualified-page streams are independent of `sessionActive`, and the
20-second checkpointing path was not the defect.

The bounded fix uses `rememberUpdatedState` for `uiState`, `viewerState`, and
`showRecoveryDevDialog`, so the same observer reads current values on every
resume. It applies the existing five-factor predicate: viewer present, PDF
document present, not loading, not unavailable, and no recovery development
dialog. Initial asynchronous viewer readiness, `ON_PAUSE` false/flush behavior,
and the actual screen-interactive checks are preserved. No coordinator, timer,
ledger, Room, protocol, permission, or session-binding change was made.

## 5. Contract decision

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
Routine opaque IDs even though the protocol version is `1`. Reader code `5` is
the minimum build for opaque IDs; code `6` is the current coordinated build and
also contains the foreground-resume visibility fix (`versionName 1.3.1`).

## 6. Source mapping

| Concern | Path | Change |
|---|---|---|
| ID predicate | `app/src/main/java/com/terinit/rhythmicreader/integration/rhythmic/RecoveryRequest.kt` | Keep `isValidUuid`; add `isValidSessionId`; `validate` uses it |
| Entry event | `app/src/main/java/com/terinit/rhythmicreader/app/MainActivity.kt` | Emit one-shot request only after exact restore succeeds |
| Entry route | `app/src/main/java/com/terinit/rhythmicreader/app/ReaderApp.kt` | Open Focus unless already on Reader |
| Resume visibility | `app/src/main/java/com/terinit/rhythmicreader/feature/reader/ReaderScreen.kt` | `rememberUpdatedState` for current UI/viewer/dialog state in the lifecycle observer |
| Version | `app/build.gradle.kts` | `versionCode 6`; `versionName 1.3.1` |

Unchanged production surfaces: Manifest/permissions, signing, Room schema,
DAO/entity/provider contract, repository acceptance and replay comparison,
qualification/timing engine, Focus/RecoveryCard UI, and existing records.

## 7. Focused checks — NOT RUN by source author

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
- Returning from background with the same PDF visible restores reader
  visibility and continues V2 credit without closing/reopening the book.
- Repeated background/return cycles do not duplicate credit; `ON_PAUSE` still
  clears visibility and flushes pending evidence.
- Signed in-place upgrade from code `5` to code `6` preserves the existing
  bound Reader session, row, and progress.

No physical, signing, or acceptance result is claimed. Code `6` full
119-test/unit/build verification and post-install foreground-resume checks are
Root-owned and were not run by the source author.

## Root verification — code 5 (3 November 2026)
Root independently ran the complete unit suite and debug assembly for the code `5` opaque/bound-entry correction: 119 tests, zero failures/errors/skips; BUILD SUCCESSFUL in 2m39s. Product source hashes remained unchanged during those checks. Evidence: c3-reader-handoff-precommit-r1.json and matching build log. Independent review, original-signer qualification and physical entry/retry tests for that correction remain as recorded separately.

Code `6` foreground-resume verification, signed in-place installation, and
post-install background/return credit preservation are not claimed here.


Root actual verification (3 November, 19:18 SAST): complete119 unit tests passed with zero failures/errors/skips, and debug assembly passed in1m47s. Product source hashes stayed unchanged during checks. Evidence c3-reader-resume-precommit-r1.json and log. Same independent reviewer follow-up and actual code6 post-install resume checks remain pending.

