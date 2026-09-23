# Rhythmic Reader Architecture

Rhythmic Reader is built with a clean, decoupled architecture: it functions completely as an independent, local-first offline PDF reader, while also providing a strictly isolated recovery evidence engine for **Rhythmic Routine**.

---

## 1. Architectural Division of Responsibility

A core architectural invariant is the strict separation between policy authority and evidence authority:

$$\begin{aligned}
\textbf{Rhythmic Routine} &\implies \textbf{Policy Authority} \quad (\text{schedules, app blocks, cooldown enforcement, re-entry decisions}) \\
\textbf{Rhythmic Reader} &\implies \textbf{Evidence Authority} \quad (\text{active reading time, qualified page dwell, proof of completion})
\end{aligned}$$

- **Reader never unlocks applications.**
- **Reader never shortens cooldowns.**
- **Reader never issues access leases.**
- **Recovery completion is evidence, not a tradeable currency.**

---

## 2. System Architecture Diagram

```text
┌────────────────────────────────────────────────────────┐
│                   Rhythmic Routine                     │
│               [Policy & Enforcement Authority]         │
└───────────────────────────┬────────────────────────────┘
                            │
              1. Local Intent (Explicit)
              com.terinit.rhythmicreader.action.START_RECOVERY
              (Signature permission protected)
                            ▼
┌────────────────────────────────────────────────────────┐
│                   Rhythmic Reader                      │
│            [Reading & Recovery Engine]                 │
│                                                        │
│  ┌──────────────────┐          ┌────────────────────┐  │
│  │   UI Layer       │          │ Reading Evidence   │  │
│  │  - LibraryView   │          │  - ActiveTracker   │  │
│  │  - TodayView     │◄────────►│  - PageEngine      │  │
│  │  - FocusView     │          │  - Daily Ledger    │  │
│  │  - ImmersivePDF  │          │  - Coordinator     │  │
│  └────────┬─────────┘          └─────────┬──────────┘  │
│           │                              │             │
│           ▼                              ▼             │
│  ┌──────────────────┐          ┌────────────────────┐  │
│  │  AndroidX PDF    │          │    Room Database   │  │
│  │ (Isolated Proc)  │          │ (rhythmic-reader)  │  │
│  └──────────────────┘          └─────────┬──────────┘  │
│                                          │             │
└──────────────────────────────────────────┼─────────────┘
                                           │
                     2. Query Recovery Status (V1)
                     content://com.terinit.rhythmicreader.recovery/sessions/{id}
                     3. Query Daily Evidence (V2)
                     content://com.terinit.rhythmicreader.evidence/daily/{dateKey}
                             (Signature permission protected)
                                           ▼
┌────────────────────────────────────────────────────────┐
│                   Rhythmic Routine                     │
│         [Verifies evidence → Unlocks re-entry]         │
└────────────────────────────────────────────────────────┘
```

---

## 3. Core Subsystems

### A. Standalone Reader & Document Management
- **Storage Access Framework (SAF):** Users choose PDF files using Android's system file picker (`ACTION_OPEN_DOCUMENT`). Rhythmic Reader requests scoped `FLAG_GRANT_READ_URI_PERMISSION` so the document remains in its original location.
- **Document Metadata & Persistence:** `BookRepository` maps files into `BookEntity` records stored in Room (`rhythmic-reader.db`). Attributes include:
  - Unique SHA-256 derived book ID
  - Display title and file size
  - Total page count
  - Last read page index (updated on navigation)
  - Last read timestamp

### B. PDF Rendering Boundary
- PDF documents are rendered using **AndroidX PDF** (`androidx.pdf:pdf-viewer-fragment`).
- AndroidX PDF executes rendering inside an isolated sandboxed process (`:pdfDocumentService`). `RhythmicReaderApplication` guards database and container initialization to ensure background rendering processes remain lightweight and decoupled.

### C. Immersive Fullscreen Focus
- Fullscreen reader mode renders the PDF across 100% of the display edge-to-edge.
- Discrete, translucent floating controls provide single-page stepping (`<` `Page X of Y` `>`) to focus attention on one page at a time without toolbar distractions.
- Animated overlays smoothly display the page scrubber and recovery status badge on user interaction.

### D. Reading Evidence & Qualification Rules
The shared reading engine validates that the user genuinely engaged with the text, rejecting superficial scrolling:

1. **Active-Time Qualification (`ActiveReadingTracker`):**
   - Tracks real elapsed time using a monotonic clock (`MonotonicClock`).
   - Requires screen to be interactive (`ScreenStateReader`), reader composable visible, document actively loaded, and application in the foreground (`LifecycleEventObserver`).
   - Accrual immediately pauses when the app is backgrounded, the screen turns off, or a dialog obscures reading.

2. **Page-Qualification Engine (`PageQualificationEngine`):**
   - A page only qualifies when the user dwells continuously on it for at least **15 seconds** (`QualificationPolicy.MIN_PAGE_DWELL_SECONDS = 15`).
   - **Rapid-Flip Protection:** Quickly scrolling through pages does not qualify them.
   - **Double-Counting Prevention:** Revisiting an already-qualified page does not increment the qualified page count.

3. **Dual-Factor Completion Rule:**
    $$\text{Session Complete} \iff (\text{Active Seconds} \ge \text{Target Seconds}) \land (\text{Qualified Pages} \ge \text{Target Pages})$$

4. **Daily Evidence Projection:** The same verified-reading deltas are recorded in the local-date ledger even when no recovery session exists. If a recovery session is active, the same measured event also updates that session; there is still only one reading timer and one page-qualification engine.

---

## 4. Cross-App IPC & Security Boundary

Inter-process communication between Rhythmic Routine and Rhythmic Reader uses standard, audited Android primitives:

1. **Signature Permission:**
   Both apps declare and consume a custom signature-level permission:
   ```xml
   <permission
       android:name="com.terinit.rhythmicreader.permission.RECOVERY"
       android:protectionLevel="signature" />
   ```
   Android OS enforces that only apps signed by the identical private key certificate can send start intents or query recovery data.

2. **Explicit Recovery Launch Intent:**
   Rhythmic Routine triggers recovery via an explicit Intent:
   - Action: `com.terinit.rhythmicreader.action.START_RECOVERY`
   - Component: `com.terinit.rhythmicreader/.integration.rhythmic.RecoveryEntryActivity`
   - Payload: Extras specifying `session_id`, `required_active_minutes`, and `required_qualified_pages`.

3. **Read-Only Recovery ContentProvider (V1):**
    Rhythmic Routine queries durable recovery session status at:
    `content://com.terinit.rhythmicreader.recovery/sessions/{sessionId}`
    Its V1 session semantics are retained.

## 5. Daily Evidence Protocol V2

The separate exported provider `com.terinit.rhythmicreader.evidence` accepts exact local-date queries at `content://com.terinit.rhythmicreader.evidence/daily/{dateKey}`. It returns only `protocolVersion`, `dateKey`, `verifiedActiveSeconds`, `qualifiedPages`, and `updatedAtEpochMs`, protected by the shared signature permission. It exposes no document or library metadata and contains no Routine policy.

See [Daily Evidence Protocol V2](docs/protocol/RHYTHMIC_READER_DAILY_EVIDENCE_PROTOCOL_V2.md) for the complete contract.

---

## 6. Process-Death & Crash Resilience

- Active reading time and qualified pages are persisted periodically to Room SQLite.
- Upon configuration change (rotation, split-screen) or activity destruction, `saveFinalProgress()` flushes the exact page index and accumulated session dwell to disk.
- If Android kills the application process in the background, the session state is restored from Room upon relaunch.
