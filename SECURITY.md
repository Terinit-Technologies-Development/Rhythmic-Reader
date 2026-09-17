# Security Policy

Rhythmic Reader is an experimental, local-first native Android PDF reader and companion recovery provider. All document data, reading positions, and recovery sessions are stored strictly on-device without cloud infrastructure, user accounts, or remote telemetry. The security boundary focuses on Android sandbox data isolation, Storage Access Framework permissions, and signature-level IPC validation.

---

## 1. Supported Versions

| Version | Supported | Notes |
| :--- | :--- | :--- |
| **1.0.x** | **Yes** | Active experimental release line. |
| **< 1.0.0** | No | Pre-release and proof-of-concept commits are not supported. |

---

## 2. Reporting a Vulnerability

If you believe you have identified a security vulnerability or permission boundary defect:

1. **Do NOT Disclose Publicly:** Do not open public issues, discussions, or pull requests detailing the vulnerability before maintainers have had an opportunity to review it.
2. **Private Vulnerability Reporting (Preferred):** Use GitHub's **Private Vulnerability Reporting** feature on this repository.
3. **Direct Contact:** If private reporting is unavailable, contact the maintainers at `legal@terinittechnologies.com` or `security@terinittechnologies.com`.
4. **Data Hygiene:** **Never** include device passwords, auth tokens, personal messages, or private PDF documents in reports. Use synthetic reproduction PDFs and sanitized logs.

---

## 3. Response Expectations & Scope

- **SLA:** Because Rhythmic Reader is experimental software, Terinit Technologies does not offer a commercial service-level agreement (SLA) or guaranteed response window. We triage and remediate reported security issues on a best-effort basis.
- **Experimental Notice:** Rhythmic Reader is not security or DRM software. It is an offline reader and recovery evidence reporter; it does not claim to prevent deliberate tampering by device owners with root or full debug privileges.
