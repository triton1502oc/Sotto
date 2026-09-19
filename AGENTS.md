# Universal AI Agent Guidelines (AGENTS.md)

### TL;DR
All AI coding agents (Copilot, Cursor, Claude, Antigravity, etc.) working on Sotto must follow the architectural guardrails in [CONTRIBUTING.md](CONTRIBUTING.md) and execute mandatory verification steps (`./gradlew assembleDebug`) after code changes.

---

## 1. Context & Architecture

Before making changes, familiarize yourself with [CONTRIBUTING.md](CONTRIBUTING.md):
- **Domain**: Assistive Communication (AAC) for autistic teens & adults.
- **UI Policy**: Low-stimulus, dignified, 100% Jetpack Compose + Material 3. Never use legacy XML layouts or `findViewById`.
- **Data Flow**: Unidirectional data flow (State flows down, Events flow up).
- **TTS**: Tied to `ComponentActivity` lifecycle.

---

## 2. Flight Rules for AI Agents

1. **Plan Before Multi-File Changes**:
   - For non-trivial modifications across multiple files, outline an implementation plan or design summary before modifying code.
2. **Build Verification**:
   - After modifying code, always verify compilation by running:
     ```bash
     ./gradlew assembleDebug
     ```
   - If the build fails, inspect the compilation stack trace, fix the Kotlin code, and re-run until the build passes cleanly.
3. **Keep Diff Minimal & Focused**:
   - Do not refactor unrelated code or alter established dark-neutral / low-stimulus styling unless explicitly requested.
4. **Environment & Secrets**:
   - Never commit keystores, signing keys, credentials, or personal machine paths.
   - Rely on standard environment variables (`JAVA_HOME`, `ANDROID_HOME`).
