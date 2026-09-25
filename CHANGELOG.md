# Changelog

All notable changes to Rhythmic Reader are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] — 2026-09-26

Paired internal release with Rhythmic Routine v1.2.0. Distribution: private /
internal Android (direct APK); no public store publication.

### Added

- **Daily Evidence Protocol V2:** verified active reading and dwell-qualified
  pages recorded per device-local calendar day in Room, with a read-only
  ContentProvider for exact-date queries.
- **Rhythmic Routine integration:** signature-protected daily evidence and
  Routine next-cooldown quota preview consumed through the shared
  `com.terinit.rhythmicreader.permission.RECOVERY` permission.
- **Reading quota card:** today's verified active reading and qualified pages
  presented against Routine's next cooldown target, refreshed every 15 seconds.
- **Active recovery session presentation:** required vs verified progress for
  both reading time and qualified pages while Routine gates re-entry.

### Fixed

- **IPC permission denial:** Reader now requests
  `com.terinit.rhythmicreader.permission.RECOVERY`, restoring Routine
  next-quota preview reads. Unauthorized callers remain denied.

### Changed

- **Version metadata:** aligned to the paired internal release —
  `versionName = "1.2.0"`, `versionCode = 2` (next monotonic value).
- **Recovery card and reader badge:** clearer reading-progress wording.

### Validation

- Unit tests, debug lint, and `assembleDebug` pass.
- APK metadata verified: `com.terinit.rhythmicreader`, versionName 1.2.0,
  versionCode 2.
- Signature permission request verified granted on-device; provider access for
  callers without the permission remains denied.
