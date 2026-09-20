# Sotto

**TL;DR**: Sotto (meaning *"under one's breath"* or a soft voice) is a dignified, low-stimulation assistive communication (AAC) Android app designed for autistic teens and adults, speech-impaired individuals, and anyone needing one-touch Text-to-Speech (TTS).

<p align="center">
  <img src="assets/screenshot.png" alt="Sotto Interface" width="300"/>
</p>

<p align="center">
  🎬 <b>Watch Video Demo:</b> 
  <a href="https://www.youtube.com/watch?v=xpPbsiT7jks">English (YouTube)</a> • 
  <a href="https://www.youtube.com/watch?v=UBBUd36im48">Indonesian (YouTube)</a>
</p>

## Who is Sotto For?
- **Autistic Teens & Adults**: A sensory-friendly, mature dark-neutral interface with zero pediatric clutter, designed for routine communication, sensory overload, and verbal shutdowns.
- **Speech-Impaired Individuals**: Accessible one-touch speech synthesis for individuals living with apraxia, dysarthria, ALS, cerebral palsy, vocal chord paralysis, or recovering from a stroke (aphasia).
- **Situational & Temporary Speech Loss**: Anyone experiencing vocal strain, laryngitis, post-surgical recovery, severe fatigue, or needing silent communication in loud, overwhelming environments.

## Features
- **Modern Low-Stimulus UI**: Dark/Neutral Material 3 theme with high contrast text, avoiding visual clutter.
- **Zero-Distraction Layout**: The main interface has zero administrative buttons during communication, providing a safe, predictable environment.
- **Emergency Bystander Mode**: Dedicated full-width hero card for non-verbal episodes, verbal shutdowns, or speech emergencies. Features a distinct `🚨 Emergency` badge and a specialized fullscreen view with high-contrast amber styling and prominent emergency header for bystanders, doctors, or first responders.
- **Quick-Speak Bar**: Spontaneous communication bar docked at the bottom with instant speech (`🔊`), fullscreen expansion (`⛶`), and voice dictation (`🎤`) without modifying your curated phrase grid.
- **Contextual Categories**: Group and filter phrases with subtle, low-stimulus filter chips (**All**, **Emergency**, **Needs**, **Social**, **General**).
- **Instant Speech**: Tap a phrase card to speak immediately with automatic language detection for seamless multilingual playback.
- **High-Visibility Display**: Long press any card or tap the fullscreen button on the quick bar to open a full-screen, high-contrast modal for silent communication.
- **Attention Chime**: Optional pre-speech tone and haptic alert to politely capture listener attention in noisy spaces before TTS starts.
- **Customizable**: Toggle "Edit List" to add, edit, delete, and drag-and-drop to rearrange phrase cards.
- **Configurable Voice & Language**: Choose your App Language from installed TTS packages. Adjust speech rate and pitch with a live "Test Voice" preview.
- **Dual-Language Speech & On-Device Translation**: Display cards in one language (e.g. English) while speaking in another (e.g. Indonesian) with a subtle indicator (`🗣️`). Features one-tap on-device auto-translation powered by Google ML Kit (100% private, zero user text sent to the cloud) and dual-language speak controls in fullscreen view.
- **Offline Capable**: Completely local processing with zero tracking, zero accounts, and 100% privacy.

## How to Install (For Users)
1. On your Android phone, go to the [Releases](https://github.com/triton1502oc/Sotto/releases) page.
2. Scroll to the latest release and download the `.apk` file (e.g., `Sotto-v1.5.2.apk`) found under "Assets".
3. Once downloaded, open the file from your notifications or using a File Manager app.
4. Your phone may warn you about installing from "Unknown Sources". Tap "Settings" on the prompt and toggle the switch to allow your browser or file manager to install the app.
5. Tap "Install". You're ready to use Sotto!

## Documentation & Development
- **Running on Physical Device**: See [RUNNING_ON_DEVICE.md](docs/RUNNING_ON_DEVICE.md) for USB debugging and installation instructions.
- **Contributing & Architecture**: See [CONTRIBUTING.md](CONTRIBUTING.md) for design philosophy, architectural guardrails, and verification rules.

## Build (For Developers)
```bash
./gradlew assembleDebug
```

## License
This project is licensed under the GNU General Public License v3.0 (with Apple App Store Exception) - see the [LICENSE](LICENSE) file for details.
