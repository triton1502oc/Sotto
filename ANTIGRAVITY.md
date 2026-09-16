# Sotto - Assistive Communication (AAC) for Teens & Adults

## Mission & Design Philosophy
Sotto (meaning "under one's breath" / soft voice) is an assistive communication app designed for autistic teens and adults.
- Dignified, low-stimulation, mature UI.
- No cartoon clip art, no pediatric pastel palettes, and no nested visual clutter.
- Fast, accessible touch targets with high contrast.

## Technical Guardrails
- Platform: Android (Min SDK 26, Target SDK 35).
- Language: 100% Kotlin.
- UI: 100% Jetpack Compose + Material 3. Never use legacy XML layouts or `findViewById`.
- Architecture: Single Activity, unidirectional data flow (State flows down, Events flow up).
- Speech: Native Android `TextToSpeech` engine tied to the `ComponentActivity` lifecycle.

## Agent Flight Rules
- Before writing multi-file changes, produce an Implementation Plan Artifact.
- Always verify compilation after modifying code by running `./gradlew assembleDebug` in the terminal.
- If the build fails, parse the error stack trace, fix the Kotlin code, and re-verify until green.
