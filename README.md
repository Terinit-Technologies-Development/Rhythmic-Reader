# Rhythmic Reader

> A quieter way to read.

Rhythmic Reader is a **source-available**, local-first native Android PDF reader created by **Terinit Technologies**.

It can be used as an independent offline reader and also serves as the first **Recovery Provider** for [Rhythmic Routine](https://github.com/Terinit-Technologies-Development/Rhythmic-Routine).

Rhythmic Reader does not sell reading time for screen time. When paired with Rhythmic Routine, it provides durable, tamper-evident reading-completion evidence while Rhythmic Routine remains solely responsible for cooldowns, restrictions, and re-entry policy.

> [!IMPORTANT]
> **Experimental V1**
>
> Rhythmic Reader is experimental software designed for digital-wellbeing exploration and mindful reading workflows. It is not medical, psychological, healthcare, or addiction-treatment software. It does not claim or guarantee habit rewiring, neural rewiring, comprehension outcomes, or behavioral changes.

---

## What It Does

### 1. Standalone Offline PDF Reader
- **Storage Access Framework (SAF):** Import PDF documents directly using Android's native system picker (`ACTION_OPEN_DOCUMENT`). Documents remain securely in their original storage provider; no files are copied or uploaded.
- **Local Progress Persistence:** Reading position, total pages, and last viewed timestamps are recorded in a private, on-device SQLite database via Android Room.
- **True Fullscreen Immersion:** The reader view expands edge-to-edge across 100% of the display. Toolbars and chrome stay out of the way.
- **Single-Page Focus:** Floating, tactile stepping controls (`<` `Page X of Y` `>`) let you focus on one page at a time without distracting sliders or navigation drawers.
- **Zero Account & Zero Cloud:** No signups, logins, remote servers, ads, telemetry, or analytics SDKs.

### 2. Recovery Provider for Rhythmic Routine
When paired with Rhythmic Routine on the same device, Rhythmic Reader acts as a local evidence provider during routine cooldowns:

```text
Rhythmic Routine
   │
   │ 1. Local explicit launch intent
   ▼
Rhythmic Reader
   │
   │ 2. Active reading dwell + anti-skimming page qualification
   │
   │ 3. Read-only completion status (ContentProvider)
   ▼
Rhythmic Routine
   │
   ▼
Policy decision: cooldown elapsed AND recovery complete → unlock re-entry
```

- **Reader Never Unlocks Apps:** Rhythmic Reader never makes re-entry decisions or modifies restrictions.
- **Reader Never Shortens Cooldowns:** The cooldown timer is managed exclusively by Rhythmic Routine.
- **Reader Never Issues Access Leases:** Access tokens and leases do not exist in Reader.
- **No Screen-Time Currency:** Recovery reading is a calm gate, never a tradeable currency.

---

## Recovery Qualification Model

To ensure reading recovery represents genuine engagement rather than unattended scrolling:

1. **Active Reading Time:** Accrues only while the app is in the foreground, the screen is interactive, and the document is visibly displayed. Pauses immediately if the screen turns off or the app is backgrounded.
2. **Page Dwell Qualification:** A page only qualifies if the reader dwells continuously on it for at least **15 seconds**. Rapid scrolling, skimming, and flipping past pages are filtered out.
3. **Dual-Factor Gate:** Completion strictly requires reaching both target active minutes AND target qualified page count.

---

## Architecture & Privacy

| Principle | Implementation |
|---|---|
| **Privacy** | 100% local-first. No network calls, telemetry SDKs, or cloud endpoints. See [PRIVACY.md](PRIVACY.md). |
| **Document Security** | Scoped Storage Access Framework permissions. PDF contents are never inspected by background services or shared via IPC. |
| **Isolation** | PDF rendering runs inside AndroidX PDF's sandboxed isolated process (`:pdfDocumentService`). |
| **Cross-App IPC** | Protected by custom signature-level permission (`com.terinit.rhythmicreader.permission.RECOVERY`). See [Protocol V1](docs/protocol/RHYTHMIC_READER_PROTOCOL_V1.md). |
| **Design** | Full architecture documentation in [ARCHITECTURE.md](ARCHITECTURE.md). |

---

## Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose with Material 3 (warm paper & sage green palette)
- **PDF Engine:** AndroidX PDF (`androidx.pdf:pdf-viewer-fragment`)
- **Local Storage:** Room Database (SQLite) with schema migrations
- **Concurrency:** Kotlin Coroutines & Flow
- **Document Access:** Android Storage Access Framework (SAF)
- **IPC:** Custom ContentProvider + Explicit Intent with signature-level permission

---

## Requirements

- **Android Device:** Android 9.0 (API level 28) or higher
- **Development Tooling:** JDK 17+, Android SDK 36/37, Gradle 9.1+

---

## Getting Started & Building from Source

Clone the repository:

```bash
git clone https://github.com/Terinit-Technologies-Development/Rhythmic-Reader.git
cd Rhythmic-Reader
```

### Build & Test Commands

**Windows (PowerShell):**
```powershell
# Run unit test suite
.\gradlew.bat testDebugUnitTest

# Run Android Lint check
.\gradlew.bat lintDebug

# Assemble debug APK
.\gradlew.bat assembleDebug
```

**macOS / Linux:**
```bash
# Run unit test suite
./gradlew testDebugUnitTest

# Run Android Lint check
./gradlew lintDebug

# Assemble debug APK
./gradlew assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Project Status & Known Limitations

Rhythmic Reader is currently at **Experimental V1**:

- **PDF Formats:** Standard PDF documents are supported via AndroidX PDF. Password-protected or DRM-encrypted PDFs require external decryption before import.
- **Landscape Layout:** Optimized primarily for portrait handheld reading; multi-column or dual-page spread reading is planned for a future milestone.
- **Hardware Acceleration:** Requires an Android device with supported native graphics drivers for AndroidX PDF rendering.

---

## Contributing

We welcome community contributions, bug reports, and architectural reviews!

- Please review [CONTRIBUTING.md](CONTRIBUTING.md) and the [Contributor License Agreement](CONTRIBUTOR_LICENSE.md) before submitting code.
- Ensure all PRs pass the checklist in [.github/pull_request_template.md](.github/pull_request_template.md).

---

## License

Rhythmic Reader is **source-available**.

- **Personal & Non-Commercial Use:** Free personal, private, and non-commercial use, modification, and study is permitted under the [Rhythmic Reader Personal Use License 1.0](LICENSE).
- **Commercial Use:** Any commercial deployment, distribution, bundling, or SaaS operation requires a separate written commercial license from Terinit Technologies. See [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md).
- **Trademarks & Branding:** The "Rhythmic Reader" and "Terinit Technologies" names, app icons, and visual branding assets are not licensed for unrestricted reuse. See [TRADEMARKS.md](TRADEMARKS.md).
- **Security Disclosures:** For security inquiries and vulnerability reporting, see [SECURITY.md](SECURITY.md).
