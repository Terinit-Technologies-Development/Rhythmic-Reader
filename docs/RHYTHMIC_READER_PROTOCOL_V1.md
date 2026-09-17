# Rhythmic Reader IPC Protocol V1 Specification

## Overview

The **Rhythmic Reader IPC Protocol V1** establishes a secure, local, inter-process communication bridge between **Rhythmic Routine** (habit management & lock-in controller) and **Rhythmic Reader** (evidence-based PDF reading application).

### Core Architectural Invariant
- **Rhythmic Reader is an Evidence Provider**: It records and certifies active, verified reading sessions. It has **no awareness** of cooldown clocks, risk groups, or access leases.
- **Rhythmic Routine is the Policy Authority**: It initiates recovery requests, evaluates re-entry eligibility (`cooldownElapsed && recoverySatisfied`), and manages user lock-in lifecycle.

---

## 1. Security & Permissions

Both applications are signed with the same developer key. Access to the recovery entry activity and the content provider is strictly guarded by a custom Android signature-level permission.

- **Permission Name**: `com.terinit.rhythmicreader.permission.RECOVERY`
- **Protection Level**: `signature`
- **Declaration**: Defined by `com.terinit.rhythmicreader` in `AndroidManifest.xml`.
- **Usage**: Requested by `com.terinit.rhythmicroutine` via `<uses-permission android:name="com.terinit.rhythmicreader.permission.RECOVERY" />`.

---

## 2. Intent Launch Contract

Rhythmic Routine initiates or resumes a recovery reading session by sending an **explicit** Intent targeted at Rhythmic Reader's `RecoveryEntryActivity`.

### Component Details
- **Action**: `com.terinit.rhythmicreader.action.START_RECOVERY`
- **Package**: `com.terinit.rhythmicreader`
- **Activity**: `com.terinit.rhythmicreader.integration.rhythmic.RecoveryEntryActivity`
- **Flags**: `Intent.FLAG_ACTIVITY_NEW_TASK`

### Intent Extras Specification
| Extra Key | Type | Description | Defensive Validation Bounds |
|---|---|---|---|
| `recovery.session_id` | `String` | Unique session identifier | Valid UUID string format |
| `recovery.protocol_version` | `Int` | Protocol version | Must equal `1` |
| `recovery.required_seconds` | `Int` | Active reading target in seconds | Range: `60` to `21600` (1 min – 6 hrs) |
| `recovery.required_pages` | `Int` | Qualified pages target | Range: `1` to `500` |
| `recovery.created_at` | `Long` | Request creation timestamp (UTC ms) | Epoch milliseconds (> 0) |
| `recovery.expires_at` | `Long` | Session deadline timestamp (UTC ms) | Must be strictly greater than `recovery.created_at` |

### Idempotency and Parameter Conflicts
1. **Identical Request**: If a session with `session_id` already exists and its target parameters (`required_seconds`, `required_pages`, `expires_at`) match, Reader opens the existing session without resetting progress.
2. **Conflicting Request**: If `session_id` already exists but target parameters differ, the request is rejected with a logged warning, preserving the existing session state.
3. **Validation Failure**: If any bound check or UUID format validation fails, the Intent is rejected and the activity finishes safely without altering state.

---

## 3. ContentProvider Query Contract

Rhythmic Routine observes and polls recovery session status via a read-only `ContentProvider`.

### Provider Details
- **Authority**: `com.terinit.rhythmicreader.recovery`
- **Read Permission**: `com.terinit.rhythmicreader.permission.RECOVERY`
- **Exported**: `true`
- **URI Pattern**: `content://com.terinit.rhythmicreader.recovery/sessions/{sessionId}`

### Projections & Data Schema
Queries to `content://com.terinit.rhythmicreader.recovery/sessions/{sessionId}` return a single row cursor containing:

| Column Name | Type | Description | Notes |
|---|---|---|---|
| `sessionId` | `String` | The requested session UUID | Primary key |
| `protocolVersion` | `Int` | Protocol version | Always `1` |
| `status` | `String` | Current session lifecycle status | Enum: `ACTIVE`, `COMPLETE`, `ABANDONED`, `EXPIRED` |
| `activeSeconds` | `Int` | Verified active reading time in seconds | Computed as `floor(accumulatedActiveMs / 1000)` |
| `qualifiedPages` | `Int` | Total distinct qualified pages turned | Must meet anti-skimming dwell threshold |
| `completedAtEpochMs` | `Long` | Timestamp of completion (UTC ms) | `0` or null if not yet completed |

### Mutability & Security Constraints
- **Read-Only**: Any invocation of `insert()`, `update()`, or `delete()` throws a `SecurityException` or `UnsupportedOperationException`.
- **Exact Query Scope**: Queries that do not match `/sessions/{sessionId}` (such as root queries or directory listings) return an empty cursor or null.
- **Change Notifications**: Rhythmic Reader calls `context.contentResolver.notifyChange(sessionUri, null)` whenever session progress updates or state transitions occur.

---

## 4. Rhythmic Routine Re-Entry Decision Logic

Rhythmic Routine retains full sovereignty over device and application re-entry policies:

```text
reentryEligible = cooldownElapsed && (!recoveryRequired || recoveryStatus == "COMPLETE")
```

1. **Cooldown Clock**: Managed solely within Rhythmic Routine.
2. **Recovery Gate**: If the cycle requires recovery, Routine queries `RecoveryStatusProvider`. If `status != "COMPLETE"`, re-entry remains blocked even if the cooldown timer has elapsed.
3. **First Free Cycle**: Routine preserves the existing rule where the initial re-entry cycle requires no recovery session.
4. **Fail-Closed Fallback**: If Rhythmic Reader is uninstalled, unresponsive, or reports an error, Routine treats recovery as unsatisfied and keeps the gate locked.
