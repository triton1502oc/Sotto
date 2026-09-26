# Security Policy

### TL;DR
Sotto is an offline, privacy-first assistive communication app. We take the security and privacy of our users seriously. Please report potential vulnerabilities responsibly via GitHub Private Vulnerability Reporting or email.

---

## Supported Versions

We provide security updates and patches for the following versions:

| Version | Supported | Notes |
| :--- | :---: | :--- |
| `v1.5.x` | :white_check_mark: | Current active release branch |
| `main` | :white_check_mark: | Development branch |
| `< v1.5.0` | :x: | Unsupported; please update to the latest release |

---

## Offline & Privacy Architecture

Sotto is engineered with a zero-trust, privacy-first model:
- **100% Offline Execution**: Sotto does not transmit speech cards, audio, or user text to external servers.
- **Local Data Storage**: All phrase libraries and category presets reside strictly on-device in SQLite (Room) and encrypted/local Android DataStore.
- **Zero Third-Party SDKs**: No analytics, behavioral trackers, advertising networks, or remote telemetry SDKs are included in the build.
- **On-Device Translation**: Translation models (ML Kit) run locally on-device once downloaded.

---

## Reporting a Vulnerability

If you discover a security vulnerability in Sotto, please report it privately:

1. **GitHub Private Vulnerability Reporting (Preferred)**:
   - Navigate to the **Security** tab of the repository (`https://github.com/triton1502oc/Sotto/security`).
   - Click **Report a vulnerability** to open an advisory draft.

2. **Direct Email**:
   - Alternatively, email **triton1502oc@gmail.com** with details about the vulnerability.
   - Please include:
     - Steps to reproduce or proof-of-concept.
     - Affected versions and device/Android environment.
     - Any potential impact on user privacy or on-device data.

### Response Timeline
- **Initial Acknowledgment**: Within 48 hours.
- **Triage & Status Update**: Within 5 business days.
- **Patch Release & Advisory**: We coordinate coordinated disclosure once a verified fix is tested and released.

Please do not open public issues for security vulnerabilities until they have been reviewed and addressed.
