# SW-2026-005 — Reader R0 Compatibility (V1 Recovery / V2 Calendar)

**Baseline:** Rhythmic Reader `01d74fefa89791c6cb5dbd0696de835ed027acb0`, branch `codex/sw-2026-005-r0`, `versionName 1.3.0` / `versionCode 3`, `applicationId com.terinit.rhythmicreader`, minSdk 28 / targetSdk 36 / compileSdk 37.
**Baseline tests:** the controller independently executed 106 native tests: PASS, 0 failures/errors/skips, exit 0. Evidence: `reader-explicit-toolchain-unit.txt` and its XML-derived summary in the programme evidence pack. The read-only contract worker did not itself execute these tests.
**Scope:** source-only contract investigation. No production code, tests, build files, Git state, signing, or device work was changed.

## 1. Verdict

- Reader exposes **sufficient, policy-neutral** provider contracts for CD3 (V2 calendar) and CD4 Reader (V1 exact session). The blocking defect is Routine-side consumer wiring (DEF-03), not missing Reader capability.
- **No Reader APK change is yet justified.** A3 must execute the concurrent-request reproduction below before deciding whether a provider fix and conditional version bump are required.
- Two Reader-side hardening observations are real but non-blocking: unenforced/unexposed `expiresAt`, and a non-atomic duplicate-accept path. Fix only on reproduction or explicit owner scope.
- Reader holds **no** policy authority: it never grants access, never shortens cooldown, never creates morning/meditation evidence.
- No installed-APK, signature, instrumentation, or physical claim is made.

## 2. Evidence basis and limitations

Read in full: `AndroidManifest.xml`, `app/build.gradle.kts`, `integration/rhythmic/{RecoveryProtocol,RecoveryRequest,RecoveryRequestParser,RecoveryEntryActivity,RecoveryStatusProvider,DailyEvidenceProtocol,DailyEvidenceProvider}.kt`, `domain/recovery/{RecoveryCoordinator,QualificationPolicy,ActiveReadingTracker,PageQualificationEngine,DailyEvidenceRecorder}.kt`, `data/repository/DefaultRecoveryRepository.kt`, plus imported contracts (`RecoveryRepository`, `RecoverySession`, `RecoveryStatus`, `RecoveryProgress`, `RecoveryDao`, `RecoverySessionEntity`, `DailyReadingEvidenceDao`, `DailyReadingEvidenceRepository`, `DailyQualifiedPageEntity`, `LocalDateClock`, `AppContainer`, `MainActivity`).

Limitations of the contract worker: `ReaderScreen.kt` was **not** inspected, so whether a bound-recovery session is visibly distinct from the generic reader surface is **unverified**. That worker exercised no APK, keystore, fingerprint, Gradle execution, or cross-app IPC. The controller separately ran the 106-test native baseline successfully; connected instrumentation and physical IPC remain NOT RUN. Routine source was not read by this worker; DEF-03 is taken from the PRD and confirmed only on the Reader side (no Reader push, pull-only provider).

## 3. V1 recovery entry contract

| Item | Actual value |
|---|---|
| Action | `com.terinit.rhythmicreader.action.START_RECOVERY` |
| Component | `.integration.rhythmic.RecoveryEntryActivity` (exported, `android:permission=…permission.RECOVERY`) |
| Extras | `recovery.session_id`, `…protocol_version`, `…required_seconds`, `…required_pages`, `…created_at`, `…expires_at` |
| Protocol | `1` (exact match required) |
| Session ID | Canonical UUID (`UUID.fromString(id).toString().equals(id, ignoreCase=true)`) |
| Bounds | seconds 60..21600; pages 1..500; `createdAt>0`; `expiresAt>createdAt` |

- Parser requires every extra (except `session_id` via `?: null`), rejects a non-null action mismatch, accepts a null action (lenient), and accepts `required_seconds` as long or int; `required_pages`/timestamps are int/long respectively, no fallback.
- CD4 values (`1800`, `11`) pass validation. Reader does **not** enforce the 1800/11 policy minimum — bounds are transport sanity only. Routine must be the policy authority.
- Acceptance inserts `ACTIVE` with the request's `createdAt`/`expiresAt`. Replay of the same ID with **identical** `(requiredSeconds, requiredPages, expiresAt)` returns the existing session (including `COMPLETE`); any mismatch returns `null` with no mutation. `protocolVersion` and `createdAt` are not compared on replay.
- **Stable-ID constraint:** because the triple is compared exactly, Routine switch-away/switch-back must replay the *original* `expiresAt`. A recomputed expiry causes silent rejection. Persist the full binding, not just the ID.
- No acknowledgement, result, or broadcast exists. Invalid or conflicting requests finish the activity with only a log line; Routine must confirm state via the status query rather than assume launch success.
- `RecoveryEntryActivity` forwards `EXTRA_SESSION_ID` to `MainActivity`, which restores the session via `RecoveryCoordinator.restoreSession`. `RecoveryCoordinator` also attaches the newest `ACTIVE` session on cold start through `observeActiveSession`, so measurement resumes without the intent.

## 4. V1 status contract (`RecoveryStatusProvider`)

Authority `com.terinit.rhythmicreader.recovery`; URI `content://com.terinit.rhythmicreader.recovery/sessions/{sessionId}`; read-only (insert/update/delete throw); `readPermission` = signature permission.

| Column | Type | Truth |
|---|---|---|
| `sessionId` | String | Bound UUID |
| `protocolVersion` | Int | `1` |
| `status` | String | `ACTIVE` / `COMPLETE`; `INVALID` for corrupt stored value |
| `activeSeconds` | Int | `accumulatedActiveMs / 1000` from **persisted** row only |
| `qualifiedPages` | Int | Distinct `(sessionId, bookId, pageIndex)` count |
| `completedAtEpochMs` | Long | Completion wall time, `0` sentinel when null |

- Unknown session → **zero-row cursor** (not null). Malformed/unmatched URI → `null`. Missing authority or denied signature permission → `IllegalArgumentException` / `SecurityException` at the caller.
- Projection is echoed verbatim; unknown columns yield `null` values rather than an error. Consumers should request exactly `ALL_COLUMNS`.
- `PENDING`, `CANCELLED`, `ABANDONED`, `EXPIRED` exist in the enum but **no inspected code path writes them**.
- `expiresAt` is stored but **never enforced and not projected**. An expired session remains `ACTIVE` and can still reach `COMPLETE`. This observation does not authorize a new expiry policy: Routine must preserve the original full binding and the owner-approved carried obligation. Neither a recomputed expiry nor a query error may clear the gate or make a legitimate obligation impossible to finish.
- `activeSeconds` can lag by the 20 s checkpoint cadence (plus queued projection), and is lost only for unsaved deltas if the process dies mid-interval. It never over-reports. `COMPLETE` is written only after persisted evidence meets both dimensions, so `status=COMPLETE` is the trustworthy completion signal; in-progress values are advisory.
- `qualifiedPages` reflects the same session-scoped ledger used for completion; V1 pages are not reused for V2 daily pages (separate tables/keys).

## 5. V2 calendar contract (`DailyEvidenceProvider`)

Authority `com.terinit.rhythmicreader.evidence`; URI `content://com.terinit.rhythmicreader.evidence/daily/{yyyy-MM-dd}`; `protocolVersion = 2`; read+write permission declared, implementation read-only.

| Column | Type | Truth |
|---|---|---|
| `protocolVersion` | Int | `2` |
| `dateKey` | String | Exact stored key |
| `verifiedActiveSeconds` | Long | Accumulated verified seconds |
| `qualifiedPages` | Int | Distinct pages for that dateKey |
| `updatedAtEpochMs` | Long | Last evidence write |

- Strict single-date lookup: exact `LocalDate` parse with round-trip equality, no ranges, no selection/args/sort, no aggregation. Unknown/duplicate/extra projection columns → `null`. Exact date with no row → zero-row cursor.
- **Accepted-day semantics (retained):** a Reader-local calendar date keyed at civil midnight. Verified time is split at local midnight using the device `ZoneId`; pages are attributed by qualifier timestamp's local date; page identity is `(dateKey, bookId, pageIndex)` and duplicates are ignored.
- Whole-second accumulation carries a per-date sub-second remainder in memory; a process restart can lose <1 s but cannot duplicate seconds.
- No re-bucketing of historical rows after a timezone/clock change. `dateKey` is the only scope identifier; Routine must query its own accepted civil day and must not infer scope from `updatedAtEpochMs`.
- Reader does not evaluate 3600 s/36 pages; CD3 acceptance stays in Routine. Existing Reader V2 semantics must not be altered by this programme.

## 6. Trust, permission, packaging

- `com.terinit.rhythmicreader.permission.RECOVERY`, `protectionLevel="signature"`, declared and self-granted by Reader; both providers and the recovery activity are `exported` behind it. Trust is therefore **install-time certificate identity**, not package name.
- Reader declares `<queries>` for `com.terinit.rhythmicroutine.qa` and `com.terinit.rhythmicroutine` (outbound visibility only). No accessibility, package-manager, network, or cloud permissions; no services; no new broad permission surface.
- `release` reuses the local `app/debug.keystore` signing config **only if that file exists**; otherwise no signing config is applied. Source cannot prove the deployed certificate — fingerprints must be measured in C2/T19. Do not treat "same repo" as "same signer".
- `DailyEvidenceProvider` declares `writePermission` although all writes throw. Cosmetic least-privilege issue, no functional or security bypass (permission is signature-gated).

## 7. Findings (Reader side)

| ID | Finding | Severity | Owner |
|---|---|---|---|
| R1 | V1 status is complete and pull-only; DEF-03 is a missing Routine caller, not a Reader gap | P0 (programme) | Routine |
| R2 | `expiresAt` not projected or enforced; stale session can still complete | P2 | Routine (enforce own binding); Reader optional |
| R3 | `acceptExternalRequest` is check-then-insert without transaction; concurrent duplicate intents can raise `SQLiteConstraintException` (ABORT) out of `RecoveryEntryActivity` | P2 conditional | Reader only on reproduction; Routine should single-flight |
| R4 | Only `ACTIVE`/`COMPLETE` are reachable; no cancellation/expiry surface, no error code column | P3 | Routine maps taxonomy |
| R5 | Provider `query` uses `runBlocking` on the Binder thread; DB contention can stall the caller | P3 | Reader (future hardening) |
| R6 | Release signing falls back to debug keystore; signature trust must be verified per artifact | P2 (release) | QA/C2 |
| R7 | Rejection of a bad/conflicting intent is silent (log only) | P3 | Routine (verify via status) |

No finding shows Reader fabricating evidence, granting access, or corrupting the V2 accepted-day aggregate.

## 8. Minimum viable tests for T12/T13/T15/T18/T19

| Test | Location (Reader) | Assertions |
|---|---|---|
| T12 `DailyEvidenceProviderV2Test` | `app/src/test/.../integration/rhythmic/` (Robolectric) | protocolVersion=2; exact 3600 s/36 pages returned; 3599 s or 35 pages fails the Routine threshold; distinct page dedupe; midnight split; unknown date = zero row, not null; duplicate projection rejected |
| T13 `RecoveryStatusProviderV1Test` | same | only the exact bound UUID returns a row; foreign/unknown ID zero-row; `activeSeconds` floor and page count match persisted values; V2 aggregate cannot satisfy V1 (separate authority/table); `0` completion sentinel not treated as complete |
| T13 `RecoveryRequestParserTest` | same | 1800/11 accepted; each missing extra → null; wrong protocol → null; `expiresAt<=createdAt` → null; non-UUID → null |
| T15 `RecoveryIdempotencyTest` | same | identical replay returns same session (incl. completed); mismatched seconds/pages/expiry returns null without mutation; concurrent duplicate requests produce one row and no crash. Actually run the R3 reproduction, retain any failure, then fix only a proven provider defect. Do not xfail, skip, or weaken mandatory T15. |
| T18 `ProviderFailureTaxonomyTest` | unit + instrumentation | unknown session zero-row; malformed URI null; unknown column null; corrupt status → `INVALID`; missing permission/unknown authority raises (instrumentation, shared signer) |
| T19 `NeutralProjectionTest` | Robolectric + instrumented same-signer | zero-row neutral response when no gate/evidence (must be distinguishable from provider-absent failure); same-signer Routine read succeeds; mismatched signer denied. Signature rows require the C2 signer harness — **not run** |

## 9. Recommendations

1. **Reader modification remains conditional on actual reproduction.** Keep `1.3.0/code 3` unless A3 proves a provider defect and a reviewed fix changes its APK; then apply the PRD's monotonic conditional bump.
2. Route DEF-03 to Routine: query V1 on recovery-entry return, resume, cold start, and bounded polling; require exact ID + protocol 1 + `COMPLETE` + seconds/pages thresholds; treat `ACTIVE` as in-progress, zero-row as unknown, exceptions as unsatisfied-but-diagnosable.
3. Persist the full per-gate binding `(gateId, provider, sessionId, protocolVersion, requiredSeconds, requiredPages, createdAt, expiresAt)` so retry and switch-back are identical. Preserve approved carry semantics; do not introduce an expiry exemption, recreate the session, or fabricate completion.
4. Never satisfy CD3 from V1 or CD4 from V2; keep the two authorities separate exactly as the code does.
5. Single-flight launch per gate/provider in Routine to remove the R3 exposure; escalate R3 to a scoped Reader fix only if duplicate or overlapping delivery is reproduced after that.
6. Verify actual signer fingerprints at C2; treat a release build without `debug.keystore` as unsigned and therefore unusable for signature IPC.
