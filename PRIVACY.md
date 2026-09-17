# Privacy Architecture & Principles

Rhythmic Reader is designed from first principles as a **strictly local-first native Android PDF reader**.

---

## 1. Zero Cloud Backend & Zero Telemetry
* Rhythmic Reader has **no remote servers, cloud databases, or API backends**.
* There are **no user accounts, logins, or authentication profiles**.
* There are **zero third-party analytics SDKs, advertising trackers, crash reporters, or background telemetry services**.
* 100% of your reading history, document library metadata, and session dwell times remain exclusively on your physical device.

---

## 2. Document Access & Storage Access Framework (SAF)
* **Scoped Least-Privilege Access:** Rhythmic Reader never requests broad storage permissions like `MANAGE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE`.
* **User-Selected Documents:** You import PDFs explicitly using Android's system document picker (`ACTION_OPEN_DOCUMENT`).
* **Local Persistable Permissions:** The app takes a persistable URI permission (`FLAG_GRANT_READ_URI_PERMISSION`) locally via Android's `ContentResolver`. Documents remain in their original storage provider (device storage, SD card, or user-selected provider) and are never uploaded or copied to external servers.

---

## 3. On-Device Metadata Persistence
* Document metadata (file display name, byte size, page count, and last viewed page) is stored in a local SQLite database via Android Room (`rhythmic-reader.db`).
* Reading progress is saved locally when you navigate pages or close a document.
* You can completely erase your reading history or clear your library anytime directly within the **You (Settings)** tab.

---

## 4. Companion IPC & ContentProvider Boundary
* When integrated with the official companion app **Rhythmic Routine**, communication occurs strictly on-device through a signature-protected ContentProvider (`com.terinit.rhythmicreader.recovery`).
* **Zero Document Exposure:** Rhythmic Reader **never** transmits document titles, file contents, page text, or reading history through the Recovery ContentProvider.
* **Evidence-Only Query:** The ContentProvider only exposes generic session completion status (`status`, `active_seconds`, `qualified_pages`, and boolean `meets_requirement`) to verify that the user completed an active reading recovery session.
* Access to this provider is strictly restricted by Android's signature-level permission (`com.terinit.rhythmicreader.permission.RECOVERY`), preventing any unauthorized third-party application on your device from reading recovery data.
