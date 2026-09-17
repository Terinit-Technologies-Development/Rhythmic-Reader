# Contributing to Rhythmic Reader

We welcome community contributions, bug reports, and improvements to Rhythmic Reader!

Rhythmic Reader is a **source-available** project distributed under the [Rhythmic Reader Personal Use License](LICENSE) with commercial licensing managed by Terinit Technologies.

---

## 1. Contributor License Terms

Before submitting code, documentation, or design improvements, please review the [Contributor License Agreement (CLA)](CONTRIBUTOR_LICENSE.md).

Submitting a Pull Request, code patch, documentation patch, or other material for incorporation constitutes acceptance of [CONTRIBUTOR_LICENSE.md](CONTRIBUTOR_LICENSE.md).

By submitting material for incorporation, you certify that:
1. Your contribution is your own original work.
2. You grant Terinit Technologies a perpetual, transferable, sublicensable license to incorporate, redistribute, and dual-license your contribution under the Personal Use License and commercial licenses.
3. You check the CLA confirmation box in the Pull Request template.

Merely opening an issue, submitting a bug report, suggesting a feature, commenting, or participating in a community discussion does not constitute a code contribution under the CLA.

---

## 2. Reader Engineering Invariants

Every contribution must preserve the following 12 foundational engineering invariants:

1. **Local-First Operation:** The application operates entirely locally without external dependencies.
2. **No Cloud Backend:** No cloud backend, remote database, or account authentication is required for core Reader behavior.
3. **Zero Telemetry/Tracking:** Never introduce analytics SDKs, advertising frameworks, crash reporting trackers, or network beacons.
4. **Standalone Usability:** Rhythmic Reader must remain fully functional as an independent offline PDF reader without Rhythmic Routine installed.
5. **Routine Policy Authority:** Rhythmic Routine remains the sole authority for application restrictions, schedules, and re-entry policies.
6. **Evidence Reporting Only:** Rhythmic Reader reports verifiable reading-completion evidence only.
7. **No Direct Unlocking:** Rhythmic Reader must never unlock applications, alter cooldowns, or grant access leases directly.
8. **Dual-Factor Recovery Completion:** Recovery completion strictly requires satisfying both active reading time AND qualified page progression.
9. **No Screen-Time Currency:** Reading recovery never becomes a tradeable currency or fractional time credit for screen allowance.
10. **Least-Privilege Document Access:** Access to PDF files uses Android's Storage Access Framework (SAF) with scoped persistable URI permissions; never request broad file system access.
11. **Narrowly Permissioned IPC:** Cross-app IPC remains strictly signature-permissioned (`com.terinit.rhythmicreader.permission.RECOVERY`) and local.
12. **Document Content Privacy:** PDF contents and text remain private to the device and are never exposed via IPC or logged.

---

## 3. Pull Request Process

1. **Branch Off Clean Base:** Create a focused feature branch from `master`.
2. **Unit Tests:** Add or update automated test suites in `app/src/test/` to cover new behaviors or fixes.
3. **Run Quality Gates:**
   ```powershell
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat lintDebug
   .\gradlew.bat assembleDebug
   ```
   Or on Unix/macOS:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew lintDebug
   ./gradlew assembleDebug
   ```
4. **Complete the PR Template:** Fill out all checklist items in `.github/pull_request_template.md`, including confirming acceptance of the [Contributor License Agreement](CONTRIBUTOR_LICENSE.md).
