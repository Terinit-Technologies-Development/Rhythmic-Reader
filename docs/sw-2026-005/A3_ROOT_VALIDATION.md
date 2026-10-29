# SW-2026-005 A3 root validation

On 29 October 2026, Root independently reproduced concurrent identical recovery acceptance against the unchanged production repository and real Room/SQLite under Robolectric. Of three tests, two passed and one failed with `SQLiteConstraintException`, primary-key code1555 on `recovery_sessions.sessionId`. The build failed after 3m23s. This established the provider defect before production edits were authorized.

After the scoped insert-if-absent/reload/full-binding repair, Root ran fresh `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` with installed JBR21, Gradle9.3.1, the existing toolchain17, one worker and a 1 GiB heap. All 114 unit tests passed, with zero failures/errors/skips; the eight concurrency/replay tests passed. Lint and debug assembly passed. The complete invocation succeeded in 7m43s, with 34 tasks executed and 21 unchanged compilation/resource tasks reused. A test init script forced fresh test execution rather than cached unit-test results.

These are JVM/native build results, not connected instrumentation or physical acceptance. Signature trust, actual Binder concurrency, Routine's exact bound launch/status consumption and CD3/CD4 lifecycle still require the remaining integration/device gates. The debug build is not a shared-signer release artifact. Reader's version remains unchanged in this source candidate; the conditional release target is 1.3.1/code4 after source review and release preparation.

Root evidence: `a3-concurrency-reproduction.txt`, `a3-concurrency-reproduction.xml`, `a3-phase2-full-native.txt`, `a3-phase2-unit-counts.json` in the controller's evidence directory. The original failing evidence is retained. Independent pinned source/test review is still required.
