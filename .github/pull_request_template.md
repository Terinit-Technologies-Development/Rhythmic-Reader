## Description

<!-- Briefly describe the rationale, architectural design, and impact of this change. -->

---

## Architectural & Privacy Verification

Please verify that your change conforms to project invariants:

- [ ] **Focused Scope:** PR addresses a single clear concern without unrelated refactoring.
- [ ] **Local-First Privacy Preserved:** No network endpoints, cloud analytics, tracking SDKs, or telemetry have been introduced.
- [ ] **Standalone Usability Preserved:** Reader remains completely usable without Rhythmic Routine.
- [ ] **Routine Authority Boundary Preserved:** Reader does not make cooldown, restriction, or application access decisions.
- [ ] **Least-Privilege SAF Access:** Document access respects Storage Access Framework without broad storage permissions.
- [ ] **Recovery Invariant Preserved:** Recovery sessions continue to require both active reading time AND qualified page dwell.

---

## Quality Checklist

- [ ] Automated tests added or updated in `app/src/test/`.
- [ ] Unit tests pass cleanly (`.\gradlew testDebugUnitTest` / `./gradlew testDebugUnitTest`).
- [ ] Android Lint passes cleanly (`.\gradlew lintDebug` / `./gradlew lintDebug`).
- [ ] Debug APK assembles cleanly (`.\gradlew assembleDebug` / `./gradlew assembleDebug`).
- [ ] No unnecessary or broad Android permissions introduced.

---

## Contributor License Agreement (CLA)

- [ ] **I agree to the terms of the [Contributor License Agreement](CONTRIBUTOR_LICENSE.md)** and confirm that this Contribution is my original work and that Terinit Technologies may redistribute and license it under personal use and commercial licenses.
