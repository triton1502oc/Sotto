# Changelog

### TL;DR
All notable changes to the Sotto project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.5.6] - 2026-09-24

### Added
- **Multi-Line Quick-Speak View**: Typing sentences in the Quick-Speak bar now expands vertically (up to 4 lines), keeping the full sentence visible and easy to edit by tapping anywhere.
- **Save as Card from Quick-Speak**: Added a `+` button in the Quick-Speak bar to save spontaneous phrases directly into your phrase board with one tap.
- **Smart Dictation / Clear Toggle**: The input bar now shows voice dictation (`🎤`) when empty and smoothly switches to clear (`✕`) while typing, giving you more space for text.

### Fixed
- **Keyboard Positioning**: Fixed an issue where the Quick-Speak bar could jump to the top of the screen when opening the keyboard.

### Assets
- `Sotto-v1.5.6.apk`: Universal release APK for all architectures.
- `Sotto-v1.5.6-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.5.6-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.5.6-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.

## [v1.5.5] - 2026-09-22

### Added
- **Language Code Normalization**: Added normalization for 3-letter ISO-639-2 language codes (e.g. `eng`, `ind`, `deu`, `ita`) returned by system TTS engines, properly mapping them to standard ISO-639-1 tags.
- **Regional & Base Language Resolution in Auto-Translate**: Expanded on-device translation language detection to match regional tags (e.g. `de-DE`) and 3-letter engine codes against ML Kit models.
- **ABI Split APKs for Lightweight Sizing**: Enabled architecture-specific APK packaging (`arm64-v8a`, `armeabi-v7a`, `x86_64`) reducing APK download footprint by up to 60% (~13–18 MB), alongside universal APK and Android App Bundle (.aab) generation.

### Changed
- **Resource Optimization**: Enabled resource shrinking (`shrinkResources`) in release builds alongside R8/ProGuard minification.
- **Language Availability**: Ensured core app languages (English and Indonesian) are always available in the voice selector even when system TTS reports empty locales.

### Assets
- `Sotto-v1.5.5.apk`: Universal release APK for all architectures.
- `Sotto-v1.5.5-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.5.5-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.5.5-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.

## [v1.5.4] - 2026-09-21

### Added
- **Settings Gear Icon in Edit Mode**: Replaced the text button ("Voice") in edit mode with a settings gear icon for cleaner and more intuitive access to Voice & Language Settings.
- **Automated Asset Pipeline**: Added `scripts/update_assets.sh` to automate capturing screenshots and generating Google Play Store feature graphics directly from an emulator or physical device.

### Changed
- **Visual Assets**: Updated phone screenshots (`screenshot.png`, `screenshot_voice_settings.png`) and Play Store feature graphic (`play_store_feature_graphic.png`) to reflect the new settings gear icon.
- **Documentation Overhaul**: Streamlined and condensed `README.md`, `CONTRIBUTING.md`, `AGENTS.md`, `docs/PLAY_STORE_LISTING.md`, `docs/RUNNING_ON_DEVICE.md`, and `demo/README.md` with clear, concise guides and standard release procedures.

### Assets
- `Sotto-v1.5.4.apk`: Signed release APK for Android devices.

## [v1.5.3] - 2026-09-20

### Fixed
- **Quick-Speak & Speech Cards in Dual-Language Mode**: Fixed an issue where speaking from Quick-Speak or tapping speech cards in dual-language mode uttered the literal word "auto" instead of auto-translating into the secondary language.
- **Phrase Constructor & Positional Arguments**: Restored `language: String = LocaleHelper.LANG_AUTO` as the second constructor parameter in `Phrase`, preventing positional calls from assigning `"auto"` to `spokenText`.
- **Legacy Storage Sanitization**: Guarded `SharedPreferencesPhraseRepository` to automatically sanitize any legacy `"auto"` entries in `spokenText` to `null`.
- **Card Subtitles & Fullscreen Safety**: Guarded card subtitles and fullscreen modal against displaying or speaking `"auto"`.

### Added
- **On-Demand Auto-Translation for Quick-Speak & Speech Cards**: When dual-language speech is enabled and secondary language is active, Quick-Speak and un-translated speech cards now automatically translate text to the secondary language on-device using Google ML Kit before speaking.
- **Quick-Speak Loading Feedback**: Added a subtle Material 3 progress indicator inside the Speak button while translating text.

### Assets
- `Sotto-v1.5.3.apk`: Signed release APK for Android devices.

## [v1.5.2] - 2026-09-20

### Fixed
- **Single-Language UI & Behavior Simplification**: Completely suppressed and hid all dual-language UI elements and alternate speech playback when "Dual-Language Speech" / "Show language switcher" is disabled in Voice Settings (the default).
- **Card Grid & Emergency Hero**: Hid `🗣️ [spokenText]` indicator row when switcher is off.
- **Card Tap (Read-Aloud)**: Guaranteed cards speak only primary text in primary language when dual-language is disabled, even if alternate spoken text was previously saved.
- **Fullscreen Modal**: Streamlined fullscreen view to display only primary text with a single large `[ Speak Aloud ]` button when dual-language is off.
- **Add / Edit Phrase Dialog**: Hid alternate spoken text section, auto-translate button, and secondary voice input when dual-language is off.

### Assets
- `Sotto-v1.5.2.apk`: Signed release APK for Android devices.

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
