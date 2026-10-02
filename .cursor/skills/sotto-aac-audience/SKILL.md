---
name: sotto-aac-audience
description: >-
  Audience and UX guardrails for Sotto, a dignified low-stimulus AAC Android app
  for autistic teens/adults, speech-impaired and non-speaking users, and
  caregivers. Use when designing or changing Sotto UI, phrase cards, TTS flows,
  emergency/fullscreen modes, Two-Way Receptive Mode, copy/microcopy, onboarding,
  Play Store text, caregiver docs, or reviewing UX for sensory load, dignity,
  privacy, or accessibility for verbal shutdowns and aphasia/apraxia/ALS use.
---

# Sotto AAC Audience & UX

## Who this is for

Primary users (not children-first AAC tropes):

- Autistic teens and adults (sensory sensitivity, verbal shutdowns)
- Speech-impaired / non-speaking users (apraxia, dysarthria, ALS, aphasia, vocal cord paralysis)
- Situational speech loss (fatigue, post-surgery, noisy environments)
- Caregivers, family, medical staff using Two-Way Receptive Mode (`👂`)

Design for **adult dignity** and **one-touch communication under stress**. Pediatric “cute AAC” patterns are wrong for this product.

## Non-negotiables

1. **Dignified & mature** — No cartoon clip-art, pediatric palettes, mascots, or nested visual clutter.
2. **Low-stimulus & high-contrast** — Dark-neutral Material 3, calm tones, large touch targets, minimal motion.
3. **Zero-Distraction Mode** — In communication mode, hide add / edit / delete / reorder. Speak first; admin later via explicit Edit List.
4. **100% offline & private** — No accounts, analytics/tracking SDKs, or cloud uploads of phrases/audio. Prefer on-device TTS, STT, and ML Kit translation.
5. **Predictable** — Prefer obvious one-tap paths over clever multi-step flows. Cognitive and motor load stay low.

Repo philosophy source: [CONTRIBUTING.md](../../../CONTRIBUTING.md). Product flows: [docs/CAREGIVER_GUIDE.md](../../../docs/CAREGIVER_GUIDE.md).

## Interaction principles

| Principle | Do | Don't |
|---|---|---|
| One-tap speak | Tap card → TTS immediately | Require confirm dialogs before everyday speech |
| Giant silent share | Long-press / fullscreen for bystanders | Tiny text that must be read over the user’s shoulder |
| Emergency first | Keep Hero / emergency card prominent and honest | Bury crisis phrases behind menus |
| Caregiver dignity | Yes / No / Repeat / Wait as large equal buttons | Cute icons, childish reply chips, or shame language |
| Face-to-face | 180° flip keeps caregiver chrome upright | Flip the whole chrome so caregivers can’t operate it |
| Edit vs communicate | Admin only after Edit List / Done | Floating edit affordances during speak mode |
| Attention | Optional gentle chime + haptics before speech | Loud, playful, or repeated start sounds |

## Copy & language

- Address the user as a capable adult. Avoid “kid,” “fun,” “playtime,” or therapy-speak that infantilizes.
- Emergency / bystander text must be clear to strangers and first responders.
- Prefer short phrases on cards; support alternate spoken text when display language ≠ speech language.
- Caregiver prompts stay clinical-calm (“Are you in pain?”), never theatrical.

## Accessibility & motor/sensory notes

- Prefer large hit targets (≥48dp) and generous spacing—especially Yes/No/Repeat/Wait and emergency actions.
- High contrast over decorative gradients, glass, or busy backgrounds.
- Motion: functional only (state change). No decorative animation loops.
- Support dual-language display/speak and offline translation without sending text off-device.
- Assume intermittent motor precision, auditory processing delay, and visual fatigue.

## Technical alignment (when implementing)

- Kotlin + Jetpack Compose + Material 3 only (no XML UI).
- UDF: state down, events up; single Activity.
- TTS bound to `ComponentActivity` lifecycle.
- After code changes: `./gradlew assembleDebug`.
- Releases: follow CONTRIBUTING release checklist; always remind to `./gradlew installRelease` and test on a device before tagging.

## Review checklist

Before shipping UI/UX or copy changes:

- [ ] Would this feel dignified to an autistic adult in shutdown—not a children’s AAC toy?
- [ ] Can the primary action complete in one tap without admin chrome visible?
- [ ] Emergency / fullscreen / caregiver paths still obvious under stress?
- [ ] No new analytics, accounts, or network phrase/audio leakage?
- [ ] Contrast, touch size, and motion stay low-stimulus?
- [ ] Caregiver Flip still usable face-to-face with dignified replies?

## Anti-patterns (reject or redesign)

- Pediatric colors, stickers, badges, confetti, gamification
- Dense dashboards, card grids with competing CTAs, or settings sprawl on the speak surface
- Modal confirmations for routine “speak this card”
- Cloud sync, social login, or “AI features” that require sending utterances off-device
- Humor that punches down on disability or non-speaking users
- Shrinking emergency or caregiver affordances to “clean up” the UI

## When unsure

Read [docs/CAREGIVER_GUIDE.md](../../../docs/CAREGIVER_GUIDE.md) for mode behavior, then propose the calmer, more adult, more offline option.
