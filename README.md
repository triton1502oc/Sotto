# Sotto

**TL;DR**: Sotto (*"under one's breath"*) is a calm yes/no tool for caregivers to reach autistic teens & adults in meltdowns and verbal shutdowns, paired with a dignified, low-stimulation AAC speech board for self-expression.

<p align="center">
  <img src="assets/screenshot.png" alt="Sotto Interface" width="300"/>
</p>

<p align="center">
  🎬 <b>Video Demo:</b> 
  <a href="https://www.youtube.com/watch?v=UKZu5ZsdORg">English</a> • 
  <a href="https://www.youtube.com/watch?v=4CvpJkjaz-k">Indonesian</a>
</p>

---

## Origin & Mission
Sotto was created by a father to navigate overwhelming communication breakdowns and meltdowns with his autistic sons (ages 22 and 15). When sensory overload strikes, speaking, reading long sentences, or processing complex questions becomes exhausting or impossible.

**Primary Mission (Crisis Navigation for Caregivers):**  
Provide parents, caregivers, therapists, and educators with a calm, low-cognitive-load yes/no branching tool to reach loved ones during meltdowns and shutdowns, and uncover the root cause (*"Why?"*) without escalating distress.

**Secondary Purpose (Dignified Self-Expression for Autistic Teens & Adults):**  
Provide non-speaking and speech-impaired individuals with a clean, mature text-to-speech board and spontaneous quick-speak bar free of pediatric cartoons and visual clutter.

---

## Target Audience
- **Caregivers, Parents, Therapists & Teachers**: Reaching loved ones during verbal shutdowns, cognitive freezes, and meltdowns; discovering root causes through structured yes/no inquiry.
- **Autistic Teens & Adults**: Dignified self-advocacy and daily communication with high-contrast, low-stimulation phrase cards and giant fullscreen text.
- **Non-Speaking & Speech-Impaired Individuals**: Accessible one-touch speech for motor apraxia, dysarthria, ALS, stroke recovery (aphasia), or situational speech loss.

---

## Key Features

### 1. Crisis Navigation (Caregiver-First)
- **Why Finder (`🧭`)**: A guided, calm yes/no branching tree designed specifically for meltdowns and verbal shutdowns. Guides caregiver and individual through physical discomfort, sensory overload, emotional overwhelm, and task friction with large binary buttons and zero open-ended demands.
- **Private Why Log & 30-Day Trend Insights**: Automatically logs resolved crisis events locally in private app storage. Highlights top 30-day root-cause patterns and peak time-of-day triggers to help families and therapists identify recurring environmental stressors.
- **Customizable Why Tree (`🌳`)**: Personalize branching questions and resolution steps to match your loved one's specific sensory profile and communication style. Reset to clinical defaults anytime.
- **Two-Way Receptive Mode (`👂`)**: Real-time caregiver speech-to-text dictation, preset question bank, Read Aloud (`🔊`), and a 180° face-to-face screen flip (`🔄 Flip`) with 4 dignified one-touch reply buttons (`Yes`, `No`, `Repeat`, `Wait`).

### 2. Dignified Self-Expression (AAC Phrase Board)
- **Low-Stimulus UI**: Dark-neutral Material 3 theme with high contrast, calm earth tones, and zero cartoon animations or visual distractions.
- **Zero-Distraction Mode**: Administrative actions (add, edit, delete, reorder) remain hidden during communication.
- **Emergency Bystander Card**: Prominent high-contrast card and fullscreen banner for non-verbal episodes and medical personnel.
- **Quick-Speak Bar**: Docked bottom bar for spontaneous speech (`🔊`), fullscreen expand, voice dictation (`🎤`), and 1-tap card creation (`+`).
- **Contextual & Custom Categories**: Filter cards by situation (**Emergency**, **Needs**, **Social**, **Care**, **General**), or customize your own categories.
- **High-Visibility Fullscreen**: Long-press any card to show giant text for silent, readable communication in loud spaces.
- **Dual-Language & On-Device Translation**: Display cards in one language while speaking in another, with 100% offline ML Kit models.

### 3. Privacy & Offline Guarantee
- **100% Offline Core**: Zero cloud accounts, zero ads, and zero third-party tracking SDKs. Audio, phrases, Why Finder trees, and Why logs stay strictly on-device.
- **Backup & Restore v2**: Full JSON backup with selective checkboxes for phrases, Why tree, and private Why log. Easily export a customized tree to a therapist or teacher without sharing private crisis notes.
- **Optional Usability Telemetry**: Strictly opt-in (default OFF) anonymous interaction telemetry to help diagnose accessibility friction (zero text, audio, or phrase hashes collected).

---

## Installation

### For Users
1. Download the latest `.apk` from [Releases](https://github.com/triton1502oc/Sotto/releases).
2. Open the downloaded file (allow "Install Unknown Apps" if prompted by Android).
3. Tap **Install** and launch Sotto.

### For Developers & Documentation
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug
```
- **Caregiver & User Guide**: See [docs/CAREGIVER_GUIDE.md](docs/CAREGIVER_GUIDE.md).
- **Language & Localization**: See [docs/LANGUAGE_AND_LOCALIZATION.md](docs/LANGUAGE_AND_LOCALIZATION.md).
- **Device Debugging**: See [docs/RUNNING_ON_DEVICE.md](docs/RUNNING_ON_DEVICE.md).
- **Contributing & Guidelines**: See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License
Licensed under the [GNU General Public License v3.0](LICENSE) (with Apple App Store Exception).
