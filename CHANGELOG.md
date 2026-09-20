# Changelog

### TL;DR
All notable changes to the Sotto project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.5.1] - 2026-09-20

### Fixed
- **Auto-Translate Crash on Physical Devices**: Added ProGuard/R8 keep rules for Google ML Kit Translation and Play Services components, resolving class-stripping crashes in release builds.
- **Defensive Exception Shield**: Wrapped all translation and model manager calls in defensive `try-catch` blocks with main-thread callback dispatches to guarantee zero crashes under any network or initialization error.

### Added
- **Model Download Confirmation**: Informative first-time dialog explaining the one-time ~30 MB language model download and 100% offline privacy.
- **Download Progress & Offline Feedback**: Distinct progress indicators ("Downloading model (~30 MB)…" vs "Translating…") and actionable error messages when offline.
- **Language Model Management in Voice Settings**: Direct status indicator and manual pre-download button under Dual-Language Speech settings.

### Assets
- `Sotto-v1.5.1.apk`: Signed release APK for Android devices.

## [v1.5.0] - 2026-09-20

### Added
- **Dual-Language Card Read-Aloud**: Cards can now display in one language (e.g., English) while reading aloud in another (e.g., Indonesian) with a subtle muted visual indicator (`🗣️`).
- **On-Device Auto-Translate (Google ML Kit)**: Instant one-tap translation from card text to secondary language using on-device ML Kit models with zero user text data transmission.
- **Fullscreen Dual-Language Speech**: Fullscreen card view now provides dedicated speak buttons for both primary and secondary languages with dynamic TTS engine switching (`en-US` / `id-ID`).
- **Top Bar Language Switcher**: Optional quick toggle pill (`[ EN | ID ]`) on the home screen to switch the active speech language across cards (configurable in Voice Settings).
- **Secondary Language Settings**: Easily configure preferred secondary speech language in Voice Settings.

### Assets
- `Sotto-v1.5.0.apk`: Signed release APK for Android devices.

## [v1.4.0] - 2026-09-19

### Added
- **Quick-Speak Bar**: Pinned bottom bar for spontaneous text-to-speech, fullscreen expand, and voice-to-text dictation docked neatly above the keyboard.
- **Emergency Hero Card & Fullscreen Mode**: Dedicated full-width emergency card with high-contrast amber styling, emergency badge, and bystander banner.
- **Contextual Categories**: Situational filter chips (All, Emergency, Needs, Social, General) with streamlined add/edit card dialog.
- **Research-Backed Default Phrases**: Streamlined 9-card default set across all categories using concise, dignified phrasing in English and Indonesian.
- **Attention Chime & Haptics**: Gentle two-tone musical chime synthesized on media stream with vibration alert, configurable in Voice settings (off by default).

### Assets
- `Sotto-v1.4.0.apk`: Signed release APK for Android devices.

## [v1.3.5] - 2026-09-18

### Added
- **Calming Sage Green Icon**: Updated app icon and Google Play store assets to an ASD-friendly, low-stimulus sage gradient with high contrast on black backgrounds.

### Fixed
- **Safe Feedback Navigation**: Protected feedback URL launch against `ActivityNotFoundException` when no browser or intent handler is available.
- **Voice Input Prompt Fix**: Resolved localized string resource lookup for speech recognition prompts.

### Changed
- **Repository & Docs Update**: Updated repository URLs and documentation to `triton1502oc/Sotto`.

### Assets
- `Sotto-v1.3.5.apk`: Signed release APK for Android devices.
