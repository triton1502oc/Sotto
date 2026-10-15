# Changelog

### TL;DR
All notable changes to the Sotto project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.7.4] - 2026-10-15

### Added
- **Direct Crisis Shortcut to Talk Mode**: Quick Settings shade tile (`EmergencyTileService`) now routes directly to Partner Mode Talk (freeform) mode for sub-3-second direct 1-tap rapid responses (Yes / No / Wait / Not Sure / Stop) during acute sensory meltdowns and speech shutdowns ([#73](https://github.com/triton1502oc/Sotto/issues/73)).
- **Keyboard Done Action in Phrase Editor**: Added `ImeAction.Done` to phrase and spoken text fields, allowing users to save phrase additions and edits directly from the soft keyboard checkmark key ([#75](https://github.com/triton1502oc/Sotto/issues/75)).

### Changed
- **Compact Category Strip & Single Save Action in Phrase Editor**: Replaced multi-line wrapping category chips with a compact 1-line horizontal scrolling row and tightened vertical spacing, ensuring dialog content and the primary Material 3 Save button remain visible and accessible above virtual keyboards on compact screens ([#75](https://github.com/triton1502oc/Sotto/issues/75)).

### Fixed
- **Resilient Small-Screen Mode Chip Layout**: Enforced single-line rendering, ellipsis overflow, and responsive control density on compact screens (<= 360dp width or high font scaling), eliminating character-by-character vertical text wrapping on the "Talk" chip ([#74](https://github.com/triton1502oc/Sotto/issues/74)).
- **Activity Recreation on Language Switch**: Stripped `EXTRA_OPEN_PARTNER_MODE` intent extra upon consumption and during `savedInstanceState` restorations, preventing language switches in Settings from unexpectedly re-triggering Partner Mode ([#73](https://github.com/triton1502oc/Sotto/issues/73)).

### Assets
- `Sotto-v1.7.4.apk`: Universal release APK for all architectures.
- `Sotto-v1.7.4-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.7.4-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.7.4-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.7.3] - 2026-10-14

### Added
- **Value-First 3-Step Welcome Onboarding**: Replaced cold double-modal gate on fresh installs with a dignified, skippable 3-step onboarding flow (Role & Intent, Emergency Safety Setup, Ready) with deferred telemetry consent until initial value delivery ([#70](https://github.com/triton1502oc/Sotto/issues/70)).
- **Interactive Emergency Safety Card Setup**: Tap-to-configure sheet for emergency safety cards featuring dedicated numeric keyboard input, natural spoken digit pacing via `LocaleHelper.formatDigitsForSpeech`, and bracket placeholder sanitization so Android TTS never vocalizes raw `[...]` placeholders aloud ([#70](https://github.com/triton1502oc/Sotto/issues/70)).
- **Upright Caregiver Mirror HUD & Screen Wake**: Dedicated upright status bar facing the caregiver in 180° flip mode displaying live selections and check-in depth; active screen wake lock (`FLAG_KEEP_SCREEN_ON`) during Partner Mode; safe exit confirmation protecting active check-ins; and remembered tab preference ([#68](https://github.com/triton1502oc/Sotto/issues/68)).
- **Quick Settings Tile Discovery**: Guided prompt on API 33+ following Partner Mode sessions offering direct notification shade tile access for sub-3-second emergency launch ([#69](https://github.com/triton1502oc/Sotto/issues/69)).
- **Graceful 3-Tier Speech Recognition Fallback**: Resilient speech engine with seamless automatic recovery across in-app speech recognizer, system intent, and localized offline voice model installation guidance ([#63](https://github.com/triton1502oc/Sotto/issues/63)).

### Changed
- **Calming Terracotta Crisis Palette & Vector Icons**: Replaced alarming red with a dignified, WCAG AAA compliant terracotta container (`#5A3030`) for "No"; standardized 80dp Yes/No dock with tactile haptics; and replaced text labels with crisp vector icons for Wait (`ic_hourglass`), Not Sure (`ic_help`), and Stop (`ic_stop_circle`) ([#72](https://github.com/triton1502oc/Sotto/issues/72)).
- **Streamlined Top Bar & Vertical Space Reclamation**: Replaced duplicate sub-headers with an elevated high-visibility squircle badge for Partner Mode in the top app bar, reclaiming 44dp of vertical height for speech tiles ([#69](https://github.com/triton1502oc/Sotto/issues/69)).
- **Default Compact Card Density**: Set compact 1.25:1 card density by default to maximize speech tile visibility on mobile screens ([#65](https://github.com/triton1502oc/Sotto/issues/65)).
- **Optional Top Bar PiP Shortcut**: Relocated floating PiP toggle into Voice Settings by default to prevent top-bar clutter ([#64](https://github.com/triton1502oc/Sotto/issues/64)).

### Fixed
- **Duplicate Phrase Grid Crash**: Implemented case-insensitive deduplication across Add/Edit phrase dialog, QuickSpeak save card toast, and JSON/QR backup import ([#66](https://github.com/triton1502oc/Sotto/issues/66)).
- **Why Finder Session ID Regeneration & Upsert**: Generated fresh session IDs on restart; preserved body parts and step indices on "Not Sure" intensity; resolved desynced session logs via upsert ([#67](https://github.com/triton1502oc/Sotto/issues/67)).
- **Telemetry Sanitization & Pre-Consent Data Preview**: Purged health-adjacent metrics (`bodyPart`, pain intensity) from telemetry payloads; enabled "View shared data" preview prior to opt-in; and persisted "Not Now" consent across version bumps ([#71](https://github.com/triton1502oc/Sotto/issues/71)).

### Assets
- `Sotto-v1.7.3.apk`: Universal release APK for all architectures.
- `Sotto-v1.7.3-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.7.3-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.7.3-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.7.2] - 2026-10-12

### Added
- **Picture-in-Picture (PiP) AAC Communicator**: Low-friction floating AAC mode enabling continuous communication while navigating maps, videos, or other apps. Built around Android PiP's system overlay architecture with 3 dedicated system `RemoteAction` buttons (`Yes`, `No`, `Wait`), an auto-enter capability on navigation/home, a high-contrast glanceable communicator surface, and opt-in settings toggle ([#45](https://github.com/triton1502oc/Sotto/issues/45)).
- **Material 3 Unified 4-Card Response Dock**: Elevated 2x2 grid response dock with `RoundedCornerShape(20.dp)`, tactile haptics, vector icons (`ic_check_circle`, `ic_cancel`, `ic_hourglass_empty`, `ic_replay`), and differentiated primary tonal vs secondary tonal elevation ([#51](https://github.com/triton1502oc/Sotto/issues/51), [#47](https://github.com/triton1502oc/Sotto/issues/47)).
- **Post-Resolution Trigger Logger LazyRow**: Inline horizontal chip selector following binary crisis resolution with vector iconography (`ic_note_alt`, `ic_close`) for fast on-device logging ([#52](https://github.com/triton1502oc/Sotto/issues/52), [#47](https://github.com/triton1502oc/Sotto/issues/47)).
- **Animated 180° Partner Flip Rotation**: Smooth 300ms rotational perspective flip animation for caregiver view toggling with vector iconography ([#53](https://github.com/triton1502oc/Sotto/issues/53), [#47](https://github.com/triton1502oc/Sotto/issues/47)).
- **Main Screen Modern Top App Bar & Quick Actions**: Streamlined TopAppBar with direct PiP communicator action, animated edit mode toggle pill, and simplified branding ([#54](https://github.com/triton1502oc/Sotto/issues/54), [#48](https://github.com/triton1502oc/Sotto/issues/48)).
- **Single-Row QuickSpeak Dock Capsule**: Modernized ~56dp QuickSpeak dock capsule (`RoundedCornerShape(28.dp)`) with embedded inline triggers (mic, fullscreen, clear, add card) and filled speak CTA ([#56](https://github.com/triton1502oc/Sotto/issues/56), [#48](https://github.com/triton1502oc/Sotto/issues/48)).
- **Compact Card Density Option & Modernized Phrases**: User-configurable compact card layout option (`1.25:1` aspect ratio) saving screen real estate, drag grip indicators in edit mode, and vector badges replacing system emojis ([#57](https://github.com/triton1502oc/Sotto/issues/57), [#48](https://github.com/triton1502oc/Sotto/issues/48)).
- **Categorized 3-Tab Settings Dialog**: Reorganized Voice Settings into 3 focused Material 3 tabs ("Voice & Speech", "Appearance & Safety", "Data & Backup") with session persistence and telemetry ([#59](https://github.com/triton1502oc/Sotto/issues/59), [#49](https://github.com/triton1502oc/Sotto/issues/49)).
- **Settings Vector Badges & Tool Tiles**: Replaced emoji section headers with 32dp tonal vector badges, converted secondary tools to Material 3 `ListItem` tiles with trailing chevrons ([#58](https://github.com/triton1502oc/Sotto/issues/58), [#49](https://github.com/triton1502oc/Sotto/issues/49)).
- **Modernized Sub-Dialogs**: Polished Why Log, Why Tree Editor, and Backup/Restore dialogs with vector iconography, selection tiles, and drag handles ([#60](https://github.com/triton1502oc/Sotto/issues/60), [#49](https://github.com/triton1502oc/Sotto/issues/49)).
- **Streamlined Category Strip & Category Manager**: Category filter strip with anchored customize button (`ic_tune`) and modernized modal bottom sheet dialog ([#55](https://github.com/triton1502oc/Sotto/issues/55), [#48](https://github.com/triton1502oc/Sotto/issues/48)).

### Assets
- `Sotto-v1.7.2.apk`: Universal release APK for all architectures.
- `Sotto-v1.7.2-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.7.2-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.7.2-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.7.1] - 2026-10-12

### Added
- **Shared Material Vector Icon Foundation**: Integrated 35 crisp, Apache-2.0 Material Symbols Rounded vector XML drawables in `res/drawable/` across common actions, Partner Mode, Main screen, and Settings, eliminating system emoji inconsistencies across Android OEM skins ([#61](https://github.com/triton1502oc/Sotto/issues/61)).
- **Material 3 Partner Mode Tool Capsule**: Grouped Partner Mode utility controls (`Log` and `Flip`) into a unified rounded M3 Surface capsule with active container highlighting, smooth 180° icon rotation micro-interaction, and standalone modal dismissal separation ([#46](https://github.com/triton1502oc/Sotto/issues/46)).

### Changed
- **Asymmetric Caregiver Action Dock**: Modernized Line 2 actions in `CaregiverBottomMenu` into an asymmetric layout prioritizing a prominent `Listen/Mic` hero button (`weight(1.5f)`) with clean vector iconography ([#50](https://github.com/triton1502oc/Sotto/issues/50)).
- **Modernized Edge-to-Edge Theme Compliance**: Explicitly configured `Theme.Sotto` and `Theme.Sotto.Capture` with transparent status and navigation bars, eliminating deprecated Holo/Fullscreen window flag warnings flagged by Google Play Console ([#43](https://github.com/triton1502oc/Sotto/issues/43)).

### Fixed
- **Caregiver Action Bar Label Truncation**: Fixed button label truncation on compact handsets ($\le 360\text{dp}$) and elevated font scales ($\ge 1.3\times$) by collapsing companion actions (`Speak`, `Cards`, `Type`) to icon-first controls with complete localized accessibility semantics ([#50](https://github.com/triton1502oc/Sotto/issues/50)).
- **Large-Screen Orientation & Resizability**: Lifted orientation lock restrictions on tablets and foldables ($\ge 600\text{dp}$) to allow free system rotation, and overrode ZXing `CaptureActivity` with `fullSensor` orientation to resolve Google Play Console large screen recommendations ([#44](https://github.com/triton1502oc/Sotto/issues/44)).

### Assets
- `Sotto-v1.7.1.apk`: Universal release APK for all architectures.
- `Sotto-v1.7.1-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.7.1-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.7.1-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.7.0] - 2026-10-11

### Added
- **Direct Yes/No Primary Crisis Cards ($\ge 80\text{dp}$ & Tactile Haptics)**: Real-world family validated crisis check-in dock in Partner Mode (`TwoWayConversationDialog`). Primary Yes/No buttons enforce $\ge 80\text{dp}$ touch target height with tactile haptic feedback and high-contrast typography, designed specifically for shaky or motor-impaired hands during sensory meltdowns and shutdowns ([#40](https://github.com/triton1502oc/Sotto/issues/40)).
- **Quick Settings Emergency Tile (`EmergencyTileService`)**: 1-tap direct launch into Partner Mode in under 3 seconds directly from the Android notification shade, featuring a custom Sotto speech bubble & voice equalizer vector icon and registered as `"Sotto Partner"` ([#40](https://github.com/triton1502oc/Sotto/issues/40)).
- **1-Tap Post-Crisis Pattern Trigger Logger**: Inline trigger logger chips (`Pain`, `Noise`, `Task`, `Emotion`, `Other`) following binary resolution in Partner Mode, writing directly to local on-device `WhyLog` without requiring the multi-step branching tree ([#40](https://github.com/triton1502oc/Sotto/issues/40)).
- **Natural Indonesian Spoken Digit Words**: Phone numbers in safety cards spoken via Indonesian TTS (`id-ID`) are converted directly into Indonesian digit words (`nol`, `satu`, `dua`, `tiga`...) with natural pause grouping, eliminating TTS date/month mispronunciations (e.g. `0812` being read as "8 Desember").
- **Integrated Voice Settings Shortcut in Voice Dropdown**: Simplified Speech & Voice settings by integrating `"Add more voices in Android Settings…"` directly into the Voice dropdown menu, eliminating the redundant standalone card for a cleaner, lower-stimulus settings layout.

### Changed
- **Dignified Medical Terminology Scrub**: Completely scrubbed clinical and medical terminology (`triage`, `diagnostic`, `treatment`, `de-escalation`) across user-facing documentation and UI strings, replacing them with dignified, assistive terminology (`crisis navigation`, `binary check-in`, `root cause discovery`, `calm communication`) while upholding Google Play medical disclaimer compliance ([#40](https://github.com/triton1502oc/Sotto/issues/40)).
- **Aligned Secondary Crisis Actions**: High-contrast secondary response cards (`Wait` / `Repeat` in Talk Mode vs `Not sure` / `Stop` in Guided Mode) with guaranteed accessibility semantics.

### Fixed
- **Compact Screen Adaptive Grid Layout**: Fixed phrase cards collapsing to a single column (1 per row) on compact screens ($\le 360\text{dp}$) and high-scaling display modes by reducing adaptive grid minimum size from 160dp to 140dp, maintaining a comfortable 2-column layout ([#41](https://github.com/triton1502oc/Sotto/issues/41)).
- **Partner Mode Talk Tab Quit Button Visibility**: Fixed the quit (`✕`) button being pushed off screen in Partner Mode when switching to the "Talk" tab on compact displays ($\le 360\text{dp}$) by streamlining utility controls into compact icon buttons and prioritizing global controls measurement ([#42](https://github.com/triton1502oc/Sotto/issues/42)).

### Assets
- `Sotto-v1.7.0.apk`: Universal release APK for all architectures.
- `Sotto-v1.7.0-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.7.0-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.7.0-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.6.2] - 2026-10-11

### Added
- **Instant Vocabulary Sharing via Offline QR Code Deck Generator & Camera Scanner**: Transfer vocabulary decks and categories between devices completely offline without cloud accounts or file management. Encodes compact payloads (`SOTTO:v1:...`) compatible with Sotto-iOS, paired with an integrated in-app optical scanner powered by ZXing ([#30](https://github.com/triton1502oc/Sotto/issues/30)).
- **Encrypted Why Finder Crisis Logs**: Private meltdown and verbal shutdown resolution logs are now encrypted at rest using AndroidX Security `EncryptedSharedPreferences` (AES256-GCM values, AES256-SIV keys). Includes seamless on-launch migration from legacy unencrypted logs ([#35](https://github.com/triton1502oc/Sotto/issues/35)).
- **Intelligent TTS Voice Picker & Voice Pack Guide**: Dedicated voice selection dropdown in Voice & Language Settings filtered exclusively for local, offline voices. Includes system settings shortcut (`Intent("com.android.settings.TTS_SETTINGS")`) and guidance for installing free neural voice packs ([#32](https://github.com/triton1502oc/Sotto/issues/32)).
- **Natural Digit-by-Digit Speech Pronunciation**: Automatic detection of 5+ digit sequences (phone numbers, safety contact cards) formatted with pauses and individual digit enunciation so Android TTS engines speak contact numbers intelligibly rather than reciting colossal numbers ([#29](https://github.com/triton1502oc/Sotto/issues/29)).
- **Safety Category Navigation & 1-Tap Hero Card Actions**: Restored `🛡️ Safety` as a first-class, pinned filter chip in the category bar with distinct amber accent styling, allowing users to directly filter, view, and organize all safety cards. Added dedicated `⛶ Fullscreen` (bystander display) and `🔊 Speak` buttons directly to the Safety card header for instant accessibility without motor fatigue or reliance on long-press gestures. Subcategories now enjoy clean, full-height phrase grids without intrusive single-phrase banners ([#39](https://github.com/triton1502oc/Sotto/issues/39)).
- **Inline Speech Dictation & 100% Offline Speech Recognition**: Hands-free question dictation in Two-Way Partner Mode with automatic silence detection (1.8s) and lifecycle-safe audio management, enforcing strictly on-device offline recognition via `createOnDeviceSpeechRecognizer` ([#34](https://github.com/triton1502oc/Sotto/issues/34), [#36](https://github.com/triton1502oc/Sotto/issues/36)).
- **Responsive Adaptive Grid for Tablets & Foldables**: Responsive phrase layout using `GridCells.Adaptive(minSize = 140.dp)` that dynamically scales between 2 and 5 columns depending on screen width and device orientation ([#37](https://github.com/triton1502oc/Sotto/issues/37)).
- **TalkBack Custom Actions & 48dp Minimum Touch Targets**: Full accessibility overhaul with TalkBack actions, explicit semantic labels, and guaranteed 48dp touch targets on navigation controls and tool buttons ([#38](https://github.com/triton1502oc/Sotto/issues/38)).
- **Social Marketing Reel & Script**: Added vertical 9:16 demo preview (`demo/sotto_vertical_preview.mp4`) and automated video generation script (`scripts/render_vertical_short.py`).

### Changed
- **Partner Mode Face-to-Face Orientation Lock**: Automatically locks device orientation (`SCREEN_ORIENTATION_LOCKED`) when entering 180° face-to-face partner flip mode to prevent accidental rotation, safely restoring original orientation on dismiss ([#33](https://github.com/triton1502oc/Sotto/issues/33)).
- **Automated AAB Bundle Builds in CI**: GitHub Actions release workflow now automatically packages and attaches the Google Play Android App Bundle (`app-release.aab`) alongside universal and ABI-specific APKs.

### Fixed
- **Partner Mode Talk Card Playback**: Fixed prompt card tap action in Talk mode (`CaregiverTab.FREEFORM`) erroneously reciting the first question from the Guided Why Finder tree instead of the active receptive text.
- **Context-Aware Safety Card Creation**: Fixed Add/Edit phrase dialog defaulting to `General` when adding cards while viewing the Safety category, ensuring newly saved phrases inherit `isEmergency` and `Safety` automatically ([#39](https://github.com/triton1502oc/Sotto/issues/39)).
- **Safety Card Fullscreen Touch Accessibility**: Eliminated nested `Surface` touch interception on grid safety cards so they expand to giant fullscreen text cleanly like all standard phrase cards.
- **Compose LocalContext Resource Lint Error**: Replaced indirect `context.getString()` in Compose Toast invocation with direct string resource identifier in `SottoApp.kt`.

### Assets
- `Sotto-v1.6.2.apk`: Universal release APK for all architectures.
- `Sotto-v1.6.2-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.6.2-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.6.2-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.6.1] - 2026-10-09

### Added
- **Sensory-Minded Color Themes**: Curated sensory-friendly color palettes accessible from Voice & Language Settings (`Appearance` section) to accommodate neurodivergent needs, photophobia, and astigmatism halation while upholding WCAG 2.1 AAA contrast: **Charcoal** (default low-stimulus dark neutral), **OLED Pure Black** (maximum contrast and power efficiency), **Warm Amber / Low Strain** (low blue light dark sepia), **Slate Navy** (calm focus), and **Soft Parchment** (anti-halation muted daylight). Safety semantics remain invariant and distinct across all themes ([#27](https://github.com/triton1502oc/Sotto/issues/27)).
- **Dignified Safety System & Personal Safety Card Templates**: Reframed the emergency system into a dignified, low-stimulus **Safety** system (`🛡️ Safety`). Added personal safety phrase templates for caregiver phone numbers and general neighborhood/locality orientation to protect user residential privacy. Added a "Show Safety Cards on Home" toggle in Settings to allow freeing 100% of the Home grid for everyday communication ([#28](https://github.com/triton1502oc/Sotto/issues/28)).
- **Two-Way HUD Quick Neutral Response & Graceful Exit**: Added a quick neutral response and graceful exit options to the Two-Way Receptive Mode HUD, and streamlined caregiver triage controls ([#23](https://github.com/triton1502oc/Sotto/issues/23)).
- **Telemetry Schema v3 & Tap Event Aggregation**: Optimized privacy-preserving usability tracking with schema v3, aggregating routine tap events to minimize payload size and device overhead, alongside privacy-preserving intent and back press navigation tracking and Settings adoption snapshots ([#24](https://github.com/triton1502oc/Sotto/issues/24), [#25](https://github.com/triton1502oc/Sotto/issues/25), [#26](https://github.com/triton1502oc/Sotto/issues/26)).

### Changed
- **Architecture Refactoring (SottoApp Extraction)**: Extracted the monolithic `SottoApp` composable (~2,000 LOC) out of `MainActivity.kt` into `ui/main/SottoApp.kt`, reducing `MainActivity` to a lean ~360 LOC focused strictly on Activity lifecycle, TTS management, and ViewModel wiring ([#8](https://github.com/triton1502oc/Sotto/issues/8)).
- **Category Bar Streamlining**: Removed the redundant Safety filter chip from the main screen's horizontal category bar to reduce visual clutter, while keeping Safety manageable in Edit Mode ([#28](https://github.com/triton1502oc/Sotto/issues/28)).
- **Refreshed Visual Assets & Demo Video**: Updated the Google Play Store preview video with warm, resonant baritone narration (`en-US-ChristopherNeural`), ambient soundtrack, and attention chime. Regenerated Play Store screenshots and feature graphic reflecting the new Safety badge and sensory color themes.

### Fixed
- **QuickSpeak Intent & Navigation Reliability**: Fixed QuickSpeak intent handling and back button navigation flow ([#22](https://github.com/triton1502oc/Sotto/issues/22)).

### Assets
- `Sotto-v1.6.1.apk`: Universal release APK for all architectures.
- `Sotto-v1.6.1-arm64-v8a.apk`: Optimized release APK for 64-bit ARM devices.
- `Sotto-v1.6.1-armeabi-v7a.apk`: Optimized release APK for 32-bit ARM devices.
- `Sotto-v1.6.1-x86_64.apk`: Optimized release APK for 64-bit x86 devices/emulators.
- `app-release.aab`: Android App Bundle for Google Play Store.

## [v1.6.0] - 2026-10-04

### Added
- **Why Finder Guided Crisis Tree**: Step-by-step calm yes/no branching tree designed specifically for meltdowns and verbal shutdowns. Guides caregivers and individuals through physical discomfort, sensory overload, emotional overwhelm, and routine friction with large binary touch targets and zero open-ended cognitive demands ([#12](https://github.com/triton1502oc/Sotto/issues/12), [#13](https://github.com/triton1502oc/Sotto/issues/13)).
- **Private Why Log & 30-Day Trend Insights**: Local-only crisis logging engine that tracks resolved de-escalation sessions. Automatically generates 30-day pattern insights showing category percentage shares, top specific triggers, and peak time-of-day crisis clusters to help families and therapists identify environmental stressors ([#14](https://github.com/triton1502oc/Sotto/issues/14)).
- **Customizable Question Tree Editor**: Dedicated crisis tree customizer dialog allowing families to edit questions, add personalized triggers, create custom crisis areas, or reset to clinical defaults with one tap ([#15](https://github.com/triton1502oc/Sotto/issues/15)).
- **Backup & Restore Format v2**: Enhanced backup engine with granular checkboxes to selectively export phrases, Why trees, and Why logs. Enables safe sharing of custom trees with therapists or schools while keeping private crisis logs strictly excluded by default ([#16](https://github.com/triton1502oc/Sotto/issues/16)).
- **Telemetry v2 for Product-Market Fit**: Enhanced privacy-preserving usability monitoring engine featuring anonymous app-assigned install IDs, coarse temporal bucketing (day-of-week and 4-hour window), optional user role selection, 500-event local buffer, and a transparent payload preview dialog in Voice Settings ([#19](https://github.com/triton1502oc/Sotto/issues/19)).
- **14-Day In-App Product-Market Fit Survey**: Low-stimulus Sean Ellis PMF survey prompted only after 14 days and at least 5 active days for opted-in users during calm idle states to evaluate product value and core benefits ([#20](https://github.com/triton1502oc/Sotto/issues/20)).
- **Question Tree Editor Enhancements**: Added voice dictation (`🎤`) for hands-free question entry, tap-to-clear (`✕`), sequence reordering controls, and a 3-dot overflow menu for one-tap reset to clinical defaults.

### Changed
- **Partner Mode Unification (`🤝`)**: Merged Why Finder and Two-Way Receptive Mode into a single cohesive Partner Mode hub featuring synchronized **Guided** (Why Finder crisis branching tree) and **Talk** (receptive speech-to-text dictation, preset questions, and 180° screen flip) modes.
- **Streamlined 4-Card Response Docks**: Standardized 4-card rapid response docks across both Guided (`Yes`, `No`, `Not sure`, `Stop`) and Talk (`Yes`, `No`, `Repeat`, `Wait`) modes with instant TTS vocalization and tactile haptics.
- **Centralized Semantic Color System**: Consolidated all hardcoded color literals across dialogs, cards, and crisis modes into `SottoColors` semantic tokens in `theme/Color.kt`, paired with `SottoAppTheme` in `theme/Theme.kt` for visual maintainability and high contrast compliance ([#9](https://github.com/triton1502oc/Sotto/issues/9)).
- **Project Scaffolding & Dependency Streamlining**: Removed dead Android Studio template files (`DataRepository.kt`, `MainScreen.kt`, `MainScreenViewModel.kt`, `Navigation.kt`, `NavigationKeys.kt`, `Type.kt`, and template tests), dropping unused `androidx.navigation3` and `kotlin.serialization` dependencies from the build system ([#7](https://github.com/triton1502oc/Sotto/issues/7)).
- **Caregiver-First & Speech Clarity Positioning**: Repositioned Play Store listing, README, and caregiver guide to lead with caregiver crisis navigation ("Calm yes/no tool for caregivers to reach autistic teens & adults in meltdowns, and find out why"), while elevating AAC phrase cards as a dignified voice for non-speaking individuals and those with unclear, slurred, or fatigued speech ([#17](https://github.com/triton1502oc/Sotto/issues/17)).
- **Caregiver Guide**: Expanded `docs/CAREGIVER_GUIDE.md` with in-depth crisis navigation strategies, Why Finder walkthroughs, trend interpretation, tree sharing instructions, and speech clarity support ([#17](https://github.com/triton1502oc/Sotto/issues/17)).
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
