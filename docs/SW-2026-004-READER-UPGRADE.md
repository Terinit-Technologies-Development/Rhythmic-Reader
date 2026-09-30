# SW-2026-004 — Reader in-place upgrade evidence

- **Date:** 2026-09-30
- **Device:** Xiaomi Redmi Note 13 Pro+ 5G, serial `P7J7TGKNAY8DKJ5P`, Android
  16 / API 36
- **Result:** **Reader in-place install and persisted-state check passed.**

## Baseline and upgrade

- Baseline source: `3515dcb2865b59c7d0ac96b8e64a496d9c693dfa` (Reader v1.2.0).
- Baseline APK: package `com.terinit.rhythmicreader`, versionCode `2`, versionName
  `1.2.0`.
- Updated APK: current `feat/pass-04-reader-alignment` build, same package and
  version metadata. Both APKs used the Android debug certificate with SHA-256
  `fac61745dc0903786fb9ede62a962b399f7348f0bb6f899b8332667591033b9c`.
- Both installs used `adb install -r`; no uninstall, clear-data, or database
  deletion was used for the upgrade.

## Representative persisted state

The test PDF was imported through Android's document picker while Reader v1.2.0
was installed. With Reader stopped, a representative in-progress recovery row
was then seeded in the private Room database to model a legacy CD4+ obligation:

| Field | Baseline value |
| --- | --- |
| Session status / protocol | `ACTIVE` / V1 |
| Required active time | `5400` seconds |
| Required qualified pages | `47` |
| Accumulated active time | `1800000` ms |
| Qualified-page rows | `1` |
| Book rows | `1` |

The fixture retained the imported book's actual document URI and the database's
existing daily-evidence rows. The database was Room schema version `4`.

## Post-upgrade verification

After installing and opening the updated APK, a fresh app-private database
capture showed:

- `PRAGMA integrity_check`: `ok`.
- Room schema version: `4`.
- Book rows: `1` (same title, URI, and page progress).
- Daily reading evidence rows: `2`.
- The active V1 session still required `5400` seconds / `47` pages, retained
  `1800000` ms progress and protocol version `1`.
- The session's qualified-page row remained present.
- Opening the updated app rendered the imported PDF in both Recent and Library,
  showing the preserved `1 of 1` page progress.

This verifies package replacement and persisted-state retention with the
requested legacy numbers. The active recovery fixture was seeded directly in
the private database; this run does not claim a completed 5400/47 reading
session or the full cross-app physical QA matrix. The pre-test app-data backup
remains outside the repository under the SW-2026-004 device-backup directory.
