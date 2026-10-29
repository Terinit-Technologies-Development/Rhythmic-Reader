# SW-2026-005 A3 — R3 Concurrency Reproduction and Fix

**Baseline:** Reader `d10dfa5cb8baf33dd7385ac83b2b58d6438d496e` (branch `codex/sw-2026-005-a3-reader-proof`), atop canonical `01d74fefa89791c6cb5dbd0696de835ed027acb0`; `versionName 1.3.0` / `versionCode 3`.
**Scope:** minimal Reader recovery-acceptance concurrency fix, production-path tests, and A3 docs. No Gradle, dependency, manifest, signing, version, Routine, Meditation, or device changes. The author did not execute Gradle.

## Root reproduction (reported by root, 29 Oct 2026)

```text
.\gradlew.bat :app:testDebugUnitTest --tests "com.terinit.rhythmicreader.integration.rhythmic.RecoveryRequestConcurrencyTest"
```

Root reported JBR 21 / Gradle 9.3.1, real Room DAO + SQLite under Robolectric, 28 tasks, exit 1, 3 tests / 1 failure / 0 errors / 0 skipped. `simultaneousIdenticalAcceptCreatesOnePersistedBindingWithoutException` failed with `android.database.sqlite.SQLiteConstraintException: INSERT OR ABORT ... UNIQUE constraint failed: recovery_sessions.sessionId (code 1555 SQLITE_CONSTRAINT_PRIMARYKEY)`. The two replay tests passed. Root retained the full log/XML as `rhythmic-sw2026-005/a3-concurrency-reproduction.{txt,xml}`. This is provider-repository race proof, not physical acceptance.

## Fix

- `RecoveryDao`: added `insertSessionIfAbsent` (`OnConflictStrategy.IGNORE`, returns `-1` on conflict), scoped to external acceptance. The existing `insertSession` keeps `ABORT` for unrelated `createSession` callers.
- `DefaultRecoveryRepository.acceptExternalRequest`: validate; atomically insert-if-absent; on success reload the persisted row; on conflict reload and compare the full immutable Routine-issued binding `(requiredSeconds, requiredPages, createdAt, expiresAt, protocolVersion)`. Exact match returns the existing row with its persisted progress/completion; any mismatch returns `null`.
- No `REPLACE`, reset, delete, or broad exception catch. Genuine DAO/database errors propagate. No schema or wire-protocol change; V1 remains `1`.

## Tests

`app/src/test/java/com/terinit/rhythmicreader/integration/rhythmic/RecoveryRequestConcurrencyTest.kt`

- `simultaneousIdenticalAcceptCreatesOnePersistedBindingWithoutException` — two independent repositories sharing real SQLite; both complete, same binding, one row.
- `conflictingInitialRaceAcrossIndependentRepositoriesChoosesOneImmutableWinner` — conflicting first accepts; one winner, one rejection, persisted winner unchanged.
- `exactReplayAcrossIndependentRepositoriesPreservesProgress` — exact replay preserves active time and qualified pages.
- `completedReplayPreservesCompletionAndEvidence` — completed exact replay stays `COMPLETE`; conflicting replay is rejected.
- `replayWithDifferentCreatedAtIsRejectedWithoutMutation` — `createdAt` is part of the immutable binding.
- `replayWithDifferentProtocolVersionIsRejectedWithoutMutation` — stored incompatible protocol binding is rejected.
- `invalidRequestDoesNotWrite` — validation failure performs no write.
- `databaseFailureIsNotSwallowed` — a `SQLiteException` from the DAO propagates (fault injection, no catch).

## Real component vs fixture boundary

**Real:** production `DefaultRecoveryRepository`, real Room `RecoveryDao`, real in-memory SQLite via `ReaderDatabase` under Robolectric, independent repository instances sharing one database. **Fixture only:** canonical request values; `BarrierRecoveryDao`, which delegates the real insert-if-absent and rendezvouses callers immediately before it; `FailingAcceptanceDao`, used only to prove error propagation. The concurrency fixture never fabricates a row or exception. Its rendezvous is bounded (5 s) so a serialized/transactional implementation cannot deadlock; a 30 s outer bound applies.

## Root checks still to run (NOT RUN by author)

```text
.\gradlew.bat :app:testDebugUnitTest --tests "com.terinit.rhythmicreader.integration.rhythmic.RecoveryRequestConcurrencyTest"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

## Limitations

Robolectric SQLite scheduling differs from a device/Binder path; no signature, IPC, or physical evidence is produced here. Full suite, lint, assemble, and independent review remain pending. Routine must persist and replay the full immutable binding (including `createdAt` and `protocolVersion`) for exact retries. Reader `1.3.1`/`code 4` remains a later reviewed-source decision, not part of this change.
