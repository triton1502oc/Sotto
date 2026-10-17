# Sotto User & Caregiver Guide

### TL;DR
Sotto serves a vital dual mission: a calm, low-cognitive-load **Partner Mode**—combining an immediate two-way receptive crisis check-in HUD and guided **Why Finder** root-cause investigation—for caregivers, therapists, and family members, paired with a dignity-first AAC speech board for autistic individuals, non-speaking users, and anyone with unclear or fatigued speech.

---

## 1. Quick Overview

Sotto (*"under one's breath"*) is designed for sensory-sensitive communication and calm communication:
- **Zero cartoon clutter**: Clean dark-neutral palette tailored for teens and adults.
- **Zero open-ended pressure**: When in meltdown or verbal shutdown, processing complex questions is overwhelming. Sotto reduces communication to large binary choices.
- **Support for Unclear Speech**: Offers an instant, clear voice for individuals whose natural speech is slurred, dysarthric, or exhausted under stress.
- **100% Offline & Private**: Audio, phrases, and crisis logs never leave your local device.

```
                      ┌────────────────────────────────────────┐
                      │              SOTTO APP                 │
                      ├────────────────────────────────────────┤
                      │                                        │
        ┌─────────────┴─────────────┐            ┌─────────────┴─────────────┐
        ▼                           ▼            ▼                           ▼
  PARTNER MODE (Caregiver Hub)             SELF-ADVOCACY (Daily AAC)
  ├── Talk Tab: Crisis Check-in HUD & Mic  ├── Instant 1-Tap Phrase Cards
  ├── 180° Face-to-Face Smooth Flip        ├── Safety Bystander Hero Card
  ├── 4-Card Tactile Response Dock         ├── Single-Row QuickSpeak Capsule
  └── Guided Tab: Root-Cause Decision Tree └── Floating PiP Communicator
```

---

## 2. Partner Mode: Immediate Crisis Check-in

When sensory overload, verbal shutdowns, cognitive freezes, or acute panic occur, processing multi-step questions causes **demand fatigue** and worsens distress. **Two-Way Partner Mode (Talk tab)** is Sotto's primary front-line tool for immediate crisis navigation and calm communication.

```
Caregiver asks 1 direct question ──► Giant text on screen ──► Individual taps 1 of 4 replies
(Voice Dictation or Preset Card)     (Smooth 180° Flip)       (80dp+ Tactile Dock with Haptics)
```

> [!IMPORTANT]
> **Caregiver Rule of Thumb: Talk vs. Guided**
> - **During Peak Meltdown or Acute Crisis**: Use **Talk Mode**. Ask one direct question (*"Do you want to leave?"*, *"Is it too loud?"*). Do not make an overwhelmed person navigate a multi-level tree.
> - **When Trigger is Unknown or After Calming Down**: Use **Guided Mode**. Methodically investigate physical discomfort, body regions, sensory triggers, or routine changes.

### How to Use Talk Mode During an Emergency Step-by-Step

1. **Open Partner Mode (<3s Launch)**:
   - Tap **Partner Mode** in the top app bar, or tap the **Sotto Partner** Quick Settings tile directly from the Android notification shade (with an automatic discovery prompt to add the tile).
   - An active screen wake lock (`FLAG_KEEP_SCREEN_ON`) keeps the display awake during crisis navigation without dimming or sleeping.
2. **Present One Urgent Question**:
   - **Speak Naturally (Mic)**: Tap the mic button. Sotto transcribes your voice in real time into giant high-contrast text. Tap again or pause to finish.
   - **Preset Check-in Cards (Questions)**: Tap Questions to pick instant comfort and binary checks (*"Are you in pain?"*, *"Do you need water?"*, *"I need a quiet space."*).
   - **Type Manually (Type)**: Type directly if ambient noise is too loud for speech recognition.
3. **Vocalize with Read Aloud (Speak)**:
   - If the individual has their eyes closed or head down, tap **Speak** to vocalize the prompt through the phone's speaker.
4. **Face-to-Face 180° Flip & Upright Caregiver Mirror HUD**:
   - Rest the phone on a table or between you and tap **Flip**.
   - The question text and the 4 response cards smoothly rotate 180° to face the person across from you.
   - An **Upright Caregiver Mirror HUD** keeps the question and partner responses clearly legible right-side-up from your perspective, while caregiver controls remain facing you.
5. **Tactile 4-Button Rapid Response**:
   - The individual taps one large, high-contrast elevated button:
     - `Yes` • `No` • `Wait` • `Repeat` (or `Not sure` • `Stop` in Guided)
   - Instant offline TTS voice confirmation plays aloud, tactile haptics pulse, and a visual confirmation badge appears.
6. **Safe Exit Confirmation & 1-Tap Pattern Logger**:
   - Tapping exit displays a safe exit confirmation prompt to prevent accidental dismissals during an ongoing crisis.
   - Following resolution, an optional horizontal strip of trigger chips (`Pain`, `Noise`, `Task`, `Emotion`, `Other`) lets caregivers record the root cause to local Why Log with a single tap.

---

## 3. Why Finder: Guided Root-Cause Discovery Tree (`🧭`)

When the immediate crisis is stabilized—or when an individual is showing signs of distress but the underlying cause is unknown (e.g. subtle physical discomfort, clothing texture, or routine friction)—switch to **Guided Mode (`🧭`)**.

**Why Finder** provides a structured, gentle yes/no decision tree designed to uncover root causes without open-ended interrogation.

### How to Use Guided Mode Step-by-Step

1. **Switch to Guided**: Tap the **Guided** (`🧭`) tab in the bottom bar of Partner Mode.
2. **Present One Question at a Time**:
   - The screen displays a single, low-stimulus question in giant text:
     `[✓ YES]` / `[✕ NO]` / `[? NOT SURE]` / `[⏹ STOP]`
   - The individual can tap, point, or nod for the caregiver to tap.
3. **Four Core Investigation Branches**:
   - **Physical / Body**: Checks for acute pain, nausea, hunger, dehydration, restroom need, sensory discomfort from temperature, or medication timing. Includes visual **Body Part Selection** and **1–5 Pain Intensity Scale**.
   - **Sensory Overload**: Checks for loud noises, bright lighting, crowds/proximity, visual chaos, irritating clothing/textures, or overwhelming smells.
   - **Emotional / Social Overwhelm**: Checks for demand fatigue, anger, sadness, anxiety, or the urgent need for solitary space.
   - **Routine & Task Friction**: Checks for unexpected schedule changes, transition shock, confusion over instructions, or physical fatigue.
4. **Resolution & Immediate Action Plan**:
   - Once a root cause is confirmed, Why Finder displays clear, concrete action steps (e.g., *"Offer quiet solitary space and dim lighting"*, *"Check temperature and offer cold water"*, *"Pause current task and remove pressure"*).
   - Tap **`🔊 Speak`** to vocalize the soothing resolution so the individual knows they have been heard and understood.
5. **Automatic On-Device Logging**:
   - Every resolved session is automatically logged to your private Why Log to help identify recurring patterns over time.
   - If the individual needs to exit at any time, tap **`⏹ Stop`** or **`✕`** — there is zero penalty or pressure.

---

## 4. Private Why Log & 30-Day Trend Insights (`📋`)

Meltdowns rarely happen without environmental triggers. The **Private Why Log** helps families and therapists understand patterns without relying on memory during stressful moments.

### Viewing Logs & Trends
- Tap **`📋 Log`** inside **Settings > Data & Backup** or from Why Finder.
- **30-Day Pattern Insights**:
  - **Category Breakdown**: See percentage shares across Sensory, Physical, Emotional, and Routine triggers (e.g. *55% Sensory Overload, 25% Physical*).
  - **Top Specific Triggers**: Identify the most frequent culprits (e.g. *"Loud noise or crowded space"*).
  - **Peak Hours of Crisis**: View time-of-day clusters (e.g. *"4:00 PM – 8:00 PM (Afternoon / Post-School)"*) to help preemptively adjust transitions and sensory environments.
- **Historical Log List**: View past events with relative time (*"2 hours ago"*, *"Yesterday"*), confirmed trigger area, specific cause, and recommended resolution.

### Privacy & Safe Sharing
- **100% Local Storage**: Why Log records are stored strictly in private, sandboxed app storage. They are never sent over the internet or included in telemetry.
- **Deduplication & 500-Entry Buffer**: Keeps local storage compact (~60 KB) and fast.
- **Selective Backup & Category Export**: When exporting a backup in **Settings > Data & Backup**, you can select specific phrase categories to export (or exclude private categories). The Why Log is **excluded by default** with an explicit privacy warning. You choose whether to include it or keep it local.

---

## 5. Customizing the Crisis Tree (`🌳`)

Every autistic individual has a unique sensory profile, communication style, and set of comfort measures.

### Using the Why Tree Editor
1. Tap **`🌳 Edit Tree`** from **Settings > Data & Backup** or inside Partner Mode.
2. **Customize Branching Questions**:
   - Tap any area (Physical, Sensory, Emotional, Routine) to view and edit its questions.
   - Tap **`+ Add Question`** to add personalized triggers (e.g., *"Is the dog barking bothering you?"*, *"Are your socks uncomfortable?"*).
   - Use voice dictation (`🎤`) to dictate questions hands-free, tap `✕` to clear text, and use sequence arrows to reorder questions.
   - Edit the specific root cause and action advice shown when resolved.
3. **Add Custom Crisis Areas**:
   - Tap **`+ Add Area`** to create entirely new inquiry areas (e.g., *"School Transitions"*, *"Medical / Therapy"*).
4. **Reset to Defaults**:
   - Tap the 3-dot overflow menu (`⋮`) and select **`Reset to Defaults`** anytime to restore the default research-backed crisis tree without losing your phrase cards.

### Sharing Trees with Therapists & Schools
- Through **Backup & Restore v2** in **Settings > Data & Backup**, you can selectively export phrase categories or **Only the Why Tree** (`Backup v2: Tree only`).
- Send the JSON file to your child's speech therapist, special education teacher, or aide.
- When they import it on their device, Sotto updates the crisis navigation tree to match your family's customized questions while keeping all private logs and personal notes completely unshared.

---

## 6. Daily Self-Advocacy AAC Guide

For autistic teens and adults, as well as individuals with unclear, slurred, or fatigued speech expressing their own thoughts during non-crisis moments:

### A. Speaking with Cards
- **Instant Speech**: Tap any phrase card once. Sotto vocalizes the text using your device's native offline Text-to-Speech (TTS) engine—eliminating listener misunderstandings and vocal fatigue.
- **Category Filter**: Use the top chips (`All`, `Safety`, `Needs`, `Social`, `Care`, `General`, or custom categories) to filter phrases for your current situation.

### B. Safety Bystander Mode
- **Hero Card**: The prominent amber/gold safety card displays your local area and emergency contact directly to bystanders or first responders.
- **Fullscreen Mode**: Long-press any card (or tap fullscreen on the Quick-Speak bar) to fill the entire screen with giant, high-contrast text. Show your screen silently to bystanders or medical personnel.

### C. Single-Row QuickSpeak Dock Capsule
- **Type Spontaneously**: Tap the docked input field (~56dp) to compose any phrase.
- **Speak**: Tap the filled speaker button to vocalize what you typed.
- **Save as Card**: While typing, tap the `+` button to save your phrase as a reusable card.
- **Voice Dictation**: Tap the microphone button to dictate phrases hands-free.
- **Fullscreen**: Tap the expand button for instant giant readable text.

### D. Floating Picture-in-Picture (PiP) Communicator
- Keep vital crisis affirmations (`Yes`, `No`, `Wait`) accessible as a floating overlay above maps, videos, or external tasks.
- Enters automatically when returning to the Home screen or switching apps (or trigger manually via Settings, with an optional top bar shortcut).

---

## 7. Customizing Phrases & Settings

### Managing Cards
1. Tap **Edit** in the top bar. The button transitions to a **Done** pill and a floating `+` action appears.
2. **Add a Card**: Tap the floating `+` button at the bottom right. Enter card text, category, and optional alternative spoken text.
3. **Edit a Card**: Tap any existing card to change its text, category, or toggle safety status.
4. **Delete a Card**: Inside the edit dialog, tap **Delete**.
5. Tap **Done** when finished.

### Categorized 3-Tab Settings
Tap the settings icon in the top app bar to configure:
- **Voice & Speech**: Speech speed, voice pitch, voice picker with Android Settings shortcut, attention chime, and dual-language translation.
- **Appearance & Safety**: Sensory color themes (Charcoal, OLED Pure Black, Warm Amber, Slate Navy, Soft Parchment), card layout density (Compact rectangular default vs classic Square), and safety bystander card visibility.
- **Data & Backup**: Instant offline QR code deck sharing, JSON v2 selective backup & restore, Why Log historical insights, Why Tree editor, and optional usability telemetry.

---

## 8. Offline Privacy & Security Guarantee

- **No Cloud Accounts**: Zero registration, accounts, or passwords.
- **No Cloud Audio Recording**: Audio data is processed strictly in real-time memory and immediately discarded.
- **Private Local Storage**: Phrases, Why trees, and crisis logs reside in sandboxed local storage on your device.
- **Zero Third-Party Trackers**: Sotto contains no commercial analytics or advertising frameworks.
