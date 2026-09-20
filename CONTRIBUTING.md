# Contributing to Sotto

### TL;DR
Sotto is an assistive communication (AAC) app designed for autistic teens and adults, speech-impaired individuals, and non-speaking users. We welcome contributions that uphold our dignified, low-stimulation design philosophy and strict Jetpack Compose / Material 3 architecture.

---

## 1. Mission & Design Philosophy

Sotto (meaning *"under one's breath"* / soft voice) is designed specifically for autistic teens, adults, speech-impaired individuals, and non-speaking users who need clear, dignified text-to-speech support.

* **Dignified & Mature UI**: Zero cartoon clip-art, zero pediatric pastel palettes, and zero nested visual clutter.
* **Low-Stimulus & High-Contrast**: Dark-neutral Material 3 theme with high contrast text and large, fast touch targets.
* **Zero-Distraction Communication**: In active communication mode, administrative actions (add, edit, delete, reorder) are hidden.
* **100% Offline & Private**: Zero accounts, zero tracking SDKs, and zero cloud uploads. All data remains on the device.

---

## 2. Technical Guardrails

* **Platform**: Android (Min SDK 26, Target SDK 35).
* **Language**: 100% Kotlin.
* **UI**: 100% Jetpack Compose + Material 3. Never use legacy XML layouts or `findViewById`.
* **Architecture**: Single Activity, unidirectional data flow (UDF: State flows down, Events flow up).
* **Speech Synthesis**: Native Android `TextToSpeech` engine tied to the `ComponentActivity` lifecycle.

---

## 3. Development Setup & Build Instructions

### Prerequisites
* **JDK 17**: Ensure `JAVA_HOME` points to JDK 17.
* **Android SDK**: Ensure `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) is configured.

### Common Commands
* **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
* **Run Unit Tests**:
  ```bash
  ./gradlew test
  ```
* **Run on Physical Device**:
  See [docs/RUNNING_ON_DEVICE.md](docs/RUNNING_ON_DEVICE.md) for USB debugging and installation instructions.

---

## 4. Release Process (Standard Pre-Release & Release Workflow)

All contributors and AI agents must follow this standard checklist before releasing a new version:

1. **Automated Verification**:
   - Run unit tests:
     ```bash
     ./gradlew testDebugUnitTest
     ```
   - Run compilation check:
     ```bash
     ./gradlew assembleDebug
     ```
2. **Version Bump**:
   - Increment `versionCode` (integer) and bump `versionName` (semver, e.g. `1.5.0`) in `app/build.gradle.kts`.
3. **Documentation Updates**:
   - Update [CHANGELOG.md](CHANGELOG.md) with new features, fixes, and release assets under the new version header.
   - Update [README.md](README.md) if new features, UI capabilities, or user workflows were added, and update the release asset download link.
   - Update [docs/PLAY_STORE_LISTING.md](docs/PLAY_STORE_LISTING.md) if user-facing store descriptions or features changed.
4. **Build Signed Release APK**:
   - Run:
     ```bash
     ./gradlew assembleRelease
     ```
   - Verify that the output APK is generated at `app/build/outputs/apk/release/Sotto-v<version>.apk`.
5. **Git Commit & Tag**:
   - Stage modified files and commit using conventional commit format:
     ```bash
     git commit -m "feat: v<version> - <summary>"
     ```
   - Push commits to main branch: `git push origin main`.
6. **GitHub Release**:
   - Publish the release using GitHub CLI:
     ```bash
     gh release create v<version> app/build/outputs/apk/release/Sotto-v<version>.apk \
       --title "v<version>" \
       --notes-file <release-notes-file>
     ```

---

## 5. Licensing & Inbound Contributions

Sotto is licensed under the [GNU General Public License v3.0](LICENSE) with an Apple App Store Exception. All contributions submitted to this repository are understood to be licensed under these same terms.
