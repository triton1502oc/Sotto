# Changelog

### TL;DR
All notable changes to the Sotto project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.6.0] - 2026-10-04

### Added
- **Why Finder Guided Crisis Tree**: Step-by-step calm yes/no branching tree designed specifically for meltdowns and verbal shutdowns. Guides caregivers and individuals through physical discomfort, sensory overload, emotional overwhelm, and routine friction with large binary touch targets and zero open-ended cognitive demands ([#12](https://github.com/triton1502oc/Sotto/issues/12), [#13](https://github.com/triton1502oc/Sotto/issues/13)).
- **Private Why Log & 30-Day Trend Insights**: Local-only crisis logging engine that tracks resolved de-escalation sessions. Automatically generates 30-day pattern insights showing category percentage shares, top specific triggers, and peak time-of-day crisis clusters to help families and therapists identify environmental stressors ([#14](https://github.com/triton1502oc/Sotto/issues/14)).
- **Customizable Question Tree Editor**: Dedicated crisis tree customizer dialog allowing families to edit questions, add personalized triggers, create custom crisis areas, or reset to clinical defaults with one tap ([#15](https://github.com/triton1502oc/Sotto/issues/15)).
- **Backup & Restore Format v2**: Enhanced backup engine with granular checkboxes to selectively export phrases, Why trees, and Why logs. Enables safe sharing of custom trees with therapists or schools while keeping private crisis logs strictly excluded by default ([#16](https://github.com/triton1502oc/Sotto/issues/16)).
- **Telemetry v2 for Product-Market Fit**: Enhanced privacy-preserving usability monitoring engine featuring anonymous app-assigned install IDs, coarse temporal bucketing (day-of-week and 4-hour window), optional user role selection, 500-event local buffer, and a transparent payload preview dialog in Voice Settings ([#19](https://github.com/triton1502oc/Sotto/issues/19)).
- **14-Day In-App Product-Market Fit Survey**: Low-stimulus Sean Ellis PMF survey prompted only after 14 days and at least 5 active days for opted-in users during calm idle states to evaluate product value and core benefits ([#20](https://github.com/triton1502oc/Sotto/issues/20)).

### Changed
- **Caregiver-First Product Positioning**: Repositioned Play Store listing, README, and documentation to lead with caregiver crisis navigation ("Calm yes/no tool for caregivers to reach autistic teens & adults in meltdowns, and find out why"), retaining mature AAC phrase cards as a secondary self-expression tool ([#17](https://github.com/triton1502oc/Sotto/issues/17)).
- **Caregiver Guide**: Expanded `docs/CAREGIVER_GUIDE.md` with in-depth crisis navigation strategies, Why Finder walkthroughs, trend interpretation, and tree sharing instructions ([#17](https://github.com/triton1502oc/Sotto/issues/17)).
- **Privacy Policy & Google Play Data Safety**: Updated `PRIVACY_POLICY.md`, `SECURITY.md`, and Play Store Data Safety specifications to reflect Telemetry v2, App-Assigned IDs, and private on-device crisis logging ([#21](https://github.com/triton1502oc/Sotto/issues/21)).

### Fixed
- **Phrase Hash Privacy Hardening**: Completely removed phrase text hash codes from card speech telemetry events to guarantee zero possibility of phrase reverse-engineering ([#18](https://github.com/triton1502oc/Sotto/issues/18)).

### Assets
- `Sotto-v1.6.0.apk`: Universal release APK for all architectures.
- `Sotto-v1.6.0-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.6.0-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.6.0-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.5.9] - 2026-10-02

### Added
- **Privacy-Preserving Usability & Telemetry Engine**: Native, zero-cost interaction monitoring engine that measures accessibility hurdles, button-to-button latency, high hesitation (>10s), and rapid/rage clicking (<350ms) using a local sandboxed JSON buffer capped at 50 events (~15 KB).
- **One-Time Install & Upgrade Consent Prompt**: One-time, low-stimulus prompt on install or new version upgrade asking users/caregivers if they want to opt in to share anonymous interaction timing, with clear and transparent zero-PII guarantees.
- **Unmetered Wi-Fi Background Sync**: Background synchronization via Android Jetpack `WorkManager` (`UsabilitySyncWorker`) with `UNMETERED` Wi-Fi and healthy battery constraints, dispatching batched payloads to private Google Apps Script webhooks with zero cellular data drain.
- **Instant Opt-Out Purge**: Toggling "Help improve Sotto" OFF in Voice & Language Settings immediately purges all pending local telemetry files from the device and cancels scheduled WorkManager jobs.

### Changed
- **Privacy Policy & Play Store Compliance**: Updated `PRIVACY_POLICY.md` and Play Store Data Safety guidelines to document the optional, strictly opt-in anonymous usability timing.

### Assets
- `Sotto-v1.5.9.apk`: Universal release APK for all architectures.
- `Sotto-v1.5.9-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.5.9-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.5.9-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.5.8] - 2026-09-27

### Added
- **Custom Category Ordering & Reorder Dialog**: Reorder categories freely via a dedicated "Manage Categories" dialog in Edit Mode, featuring drag-and-drop handles and accessible single-step reorder buttons ([#3](https://github.com/triton1502oc/Sotto/issues/3)).
- **Custom Categories & Reduced Locked Categories**: Locked categories reduced from 5 down to 2 (`Emergency` for safety and `General` as the safe fallback). `Needs`, `Social`, and `Care` are now fully customizable (can be renamed, reordered, or deleted). Deleting any category safely reassigns all contained cards to `General` ([#3](https://github.com/triton1502oc/Sotto/issues/3)).
- **Dynamic Category Pickers**: Updated card creation/editing dialog and Caregiver Receptive Mode questions picker with dynamic category layouts matching the user's custom category order ([#3](https://github.com/triton1502oc/Sotto/issues/3)).

### Changed
- **Refreshed App Launcher Icons & Mipmaps**: Refreshed launcher icons (`ic_launcher` / `ic_launcher_round`) across all dpi densities (`hdpi`, `mdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) featuring high-contrast speech acoustics iconography with proper adaptive background and foreground vector layers.
- **Updated Demo Videos & Visual Assets**: Updated video demonstrations and store assets in `assets/` and `demo/` reflecting the latest launcher iconography and UI enhancements.
- **Community Standards & Governance**: Added Code of Conduct, Security Policy, markdown issue templates, and pull request guidelines for open-source contributors.

### Fixed
- **Quick-Speak Fullscreen Icon & Affordance**: Replaced the ambiguous unicode `⛶` glyph with a crisp Material vector icon (`open_in_full`) and added an accessible Material 3 `TooltipBox`, eliminating font-dependent `[]` missing glyph box rendering ([#2](https://github.com/triton1502oc/Sotto/issues/2)).

### Assets
- `Sotto-v1.5.8.apk`: Universal release APK for all architectures.
- `Sotto-v1.5.8-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.5.8-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.5.8-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.5.7] - 2026-09-25

### Added
- **Two-Way Caregiver Receptive Mode**: Dedicated receptive HUD accessible via the ear icon (`👂`) in the top bar, allowing caregivers, medical professionals, and communication partners to speak or present questions to non-speaking users.
- **180° Face-to-Face Screen Flip**: One-tap screen inversion (`🔄 Flip`) that turns the question display and response dock upside down for conversation partners sitting across a table, while keeping caregiver controls upright.
- **Read Aloud for Questions**: Added a `🔊 Read Aloud` button in caregiver mode to vocalize the displayed question text via offline TTS for users with visual fatigue or eyes closed.
- **Categorized Question Bank**: Quick question picker with category filter chips (`Care`, `Needs`, `Social`, `Emergency`) for standard care and comfort checks.
- **Dignified 4-Button Rapid Response**: High-contrast, tactile response dock with four dignified answers (`Yes`, `No`, `Repeat`, `Wait`), each triggering instant TTS speech, visual badge confirmation, and subtle haptic feedback.
- **Streamlined Midline Top Bar**: Unified top app bar featuring Two-Way Receptive Mode (`👂`), permanent Voice & Language Settings (`⚙️`), and clean rounded `[✏️ Edit List]` action button centered on the exact horizontal midline with zero layout shifting.
- **Caregiver & User Guide**: Added comprehensive documentation in `docs/CAREGIVER_GUIDE.md` covering daily AAC cards, emergency hero mode, Quick-Speak, and caregiver two-way interactions.

### Changed
- **Visual Assets**: Rebuilt Google Play Store screenshots and feature graphic via `./scripts/update_assets.sh` reflecting the new top bar alignment and settings gear icon.

### Assets
- `Sotto-v1.5.7.apk`: Universal release APK for all architectures.
- `Sotto-v1.5.7-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.5.7-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.5.7-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store closed testing.

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
