# Changelog

All notable changes to Rhythmic Reader are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] — unreleased

Coordinated corrective build with Rhythmic Routine. Distribution remains
private/internal Android; no public store publication. V1 wire protocol remains
`1`.

### Added

- **Opaque recovery session IDs:** V1 entry now accepts stable, bounded opaque
  identifiers in addition to canonical UUIDs. IDs are persisted and queried
  exactly as supplied, without trimming or normalization. Existing UUID callers
  retain their prior behavior.
- **Bound entry flow:** an accepted explicit recovery entry routes to Focus;
  an already visible Reader surface is left in place. Both cold-start and
  `onNewIntent` delivery are covered. Ordinary launches do not route merely
  because an unrelated active row exists.

### Fixed

- **CD4 Reader entry rejection:** Reader code `4` rejected the current Routine
  `gateId + "-reader"` binding at V1 validation, before persistence. Reader
  code `5` accepts that binding and completes the existing immutable-binding
  acceptance path. Older UUID-only Reader builds reject current Routine IDs
  despite protocol `1`.
- **Foreground-resume reading credit:** the lifecycle observer previously read
  the initial `viewerState`/`uiState` values and could leave Reader marked
  invisible after returning from background while the PDF stayed visible. The
  observer now reads the latest visibility state on every resume. No timer,
  coordinator, ledger, or schema behavior changed.

### Changed

- **Version metadata:** `versionName = "1.3.1"`, `versionCode = 6`.
- No schema, permission, signing, provider URI/column, timing, or page
  semantics changed. Existing recovery records are preserved.

### Validation

- Root-owned unit tests, lint, assembly, signing, and first/repeat entry
  verification remain pending; the source author did not run them.

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
