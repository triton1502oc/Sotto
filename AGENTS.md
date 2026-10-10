# Universal AI Agent Guidelines (AGENTS.md)

### TL;DR
All AI coding agents (Copilot, Cursor, Claude, Antigravity, etc.) must adhere to the technical guardrails in [CONTRIBUTING.md](CONTRIBUTING.md) and run `./gradlew assembleDebug` after code changes.

---

## 1. Context & Architecture
Refer to [CONTRIBUTING.md](CONTRIBUTING.md) for:
- **Mission & Philosophy**: [CONTRIBUTING.md#1-mission--design-philosophy](CONTRIBUTING.md#1-mission--design-philosophy) (dignified, low-stimulus AAC).
- **Technical Guardrails**: [CONTRIBUTING.md#2-technical-guardrails](CONTRIBUTING.md#2-technical-guardrails) (Kotlin, Jetpack Compose, Material 3, UDF, TTS lifecycle).

---

## 2. Flight Rules for AI Agents

1. **Plan Before Multi-File Changes**:
   Outline an implementation plan or design summary before modifying code across multiple files.
2. **Build Verification**:
   Verify compilation after any code changes:
   ```bash
   ./gradlew assembleDebug
   ```
   If compilation fails, inspect the stack trace, fix the issue, and re-verify until green.
3. **Keep Diff Minimal & Focused**:
   Do not refactor unrelated code or modify established styling unless explicitly requested.
4. **Environment & Secrets**:
   Never commit keystores, signing keys, credentials, or personal paths. Rely on `JAVA_HOME` and `ANDROID_HOME`.
5. **Release Checklist**:
   Follow the 7-step release workflow in [CONTRIBUTING.md#4-release-process-standard-pre-release--release-workflow](CONTRIBUTING.md#4-release-process-standard-pre-release--release-workflow) (including building both APKs and AAB bundle via `./gradlew assembleRelease bundleRelease`, GitHub Milestone tracking, issue linking in `CHANGELOG.md`, and closing milestones). Use the clean release chore format: `chore(release): prepare v<version>`.
6. **Pre-Release Device Verification Prompt (MANDATORY)**:
   When assisting with any release, version bump, or release preparation, AI agents **MUST ALWAYS explicitly ask and remind the user to install and test the Release APK on a physical device or emulator** (`./gradlew installRelease`) before creating the git tag or pushing the release. Never tag or finalize a release without prompting the user.
7. **Autonomous Asset Rebuilding**:
   When requested to update or rebuild screenshots and visual assets (`assets/`), execute `./scripts/update_assets.sh` directly without requiring multi-step implementation plans or step-by-step user approvals.
8. **Issue Assignment on Close**:
   When resolving, updating, or closing a GitHub issue or ticket, if the issue is unassigned, assign it to yourself (`gh issue edit <id> --add-assignee "@me"`) before or upon closing.
9. **Simplicity, Brevity, Safety & Security (Zero Regressions)**:
   - **Ensure Nothing Breaks**: Guarantee backwards compatibility for existing user data (e.g., saved phrases, preferences) and verify test coverage (`./gradlew testDebugUnitTest`).
   - **Simpler Code & Shorter Lines of Code**: Emphasize minimal abstractions, direct logic, concise Kotlin idioms, and eliminating dead code over premature architecture.
   - **Secure Code**: Maintain 100% offline privacy guarantees (zero covert analytics/leaks), sanitize file/clipboard imports, and adhere to secure Android development standards.
10. **Telemetry for New Features**:
    Every newly added user-facing feature or configurable capability MUST include corresponding privacy-preserving, opt-in telemetry via `UsabilityTracker` (e.g., activation marker, anonymous usage count, or state snapshot). Telemetry must strictly remain anonymous, zero PII/content, and gated behind user consent (`shareUsabilityMetrics`).
