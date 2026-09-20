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
   Follow the 6-step release workflow in [CONTRIBUTING.md#4-release-process-standard-pre-release--release-workflow](CONTRIBUTING.md#4-release-process-standard-pre-release--release-workflow). Use the clean release chore format: `chore(release): prepare v<version>`.
6. **Pre-Release Device Verification Prompt (MANDATORY)**:
   When assisting with any release, version bump, or release preparation, AI agents **MUST ALWAYS explicitly ask and remind the user to install and test the Release APK on a physical device or emulator** (`./gradlew installRelease`) before creating the git tag or pushing the release. Never tag or finalize a release without prompting the user.
7. **Autonomous Asset Rebuilding**:
   When requested to update or rebuild screenshots and visual assets (`assets/`), execute `./scripts/update_assets.sh` directly without requiring multi-step implementation plans or step-by-step user approvals.

