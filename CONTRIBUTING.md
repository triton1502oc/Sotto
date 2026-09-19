# Contributing to Sotto

### TL;DR
Sotto is an assistive communication (AAC) app designed for autistic teens and adults. We welcome contributions that uphold our dignified, low-stimulation design philosophy and strict Jetpack Compose / Material 3 architecture.

---

## 1. Mission & Design Philosophy

Sotto (meaning *"under one's breath"* / soft voice) is designed specifically for autistic teens, adults, and non-speaking individuals who need clear, dignified text-to-speech support.

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

## 4. Release Process

* Releases are tracked in [CHANGELOG.md](CHANGELOG.md) and published via GitHub Releases (`gh release create`).
* Store assets and metadata are documented in [docs/PLAY_STORE_LISTING.md](docs/PLAY_STORE_LISTING.md).
