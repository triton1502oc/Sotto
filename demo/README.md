# Sotto Demo Videos

### TL;DR
High-definition demo videos and reproduction scripts showcasing Sotto's flagship assistive communication features—including **Two-Way Caregiver Receptive Mode** (180° face-to-face screen flip and rapid responses), **Dual-Language Speech** (`[ EN | ID ]` switching & bilingual emergency buttons), **Two-Tier Quick-Speak Bar**, **Emergency Bystander Hero Card**, and the **Two-Tone Attention Chime**—in both English and Indonesian.

---

## Demo Videos

| Video | Primary Lang | Secondary Lang | Primary Voice | Watch | File |
|---|---|---|---|---|---|
| **English Demo** | `en-US` | `id-ID` | Samantha | [YouTube](https://www.youtube.com/watch?v=UKZu5ZsdORg) ([Shorts](https://youtube.com/shorts/UKZu5ZsdORg)) | [`sotto_demo_en.mp4`](sotto_demo_en.mp4) |
| **Indonesian Demo** | `id-ID` | `en-US` | Damayanti | [YouTube](https://www.youtube.com/watch?v=4CvpJkjaz-k) ([Shorts](https://youtube.com/shorts/4CvpJkjaz-k)) | [`sotto_demo_id.mp4`](sotto_demo_id.mp4) |

*Specs: ~54s, 720×1600 @ 30fps, H.264 / AAC 192 kbps.*

---

## Featured Capabilities

1. **Modern Two-Tier Quick-Speak Bar**: Spontaneous text input, instant speech playback (`🔊`), and large-card fullscreen modal (`⛶`).
2. **Emergency Bystander Mode & Dual-Language**: Category filtering and emergency hero card with bilingual one-touch speech buttons.
3. **Two-Way Caregiver Receptive Mode**: Dedicated receptive HUD (`👂`), caregiver question prompt bank (`📋`), 180° face-to-face screen flip (`🔄 Flip`), and dignified rapid-response buttons (`✓ Yes` / `✕ No` / `🔁 Repeat` / `⏳ Wait`) with instant voice confirmation.
4. **Dual-Language TopBar Switcher**: Instant switching between primary and secondary speech languages with offline ML Kit translation.
5. **Voice Customization & Attention Chime**: Two-tone pre-speech alert chime (D5 587.33 Hz + A5 880.00 Hz) and configurable speech rate/pitch.

---

## Recording & Reproduction

### Prerequisites
- **Android Emulator/Device**: 1080×2400 (e.g. Pixel 8 / `medium_phone`), $\ge$ 4 GB RAM.
- **Tools**: `ffmpeg` (`brew install ffmpeg`), `adb` in `$PATH`.
- **macOS TTS Voices**: `Samantha` (default `en_US`) and `Damayanti` (`id_ID`) (*System Settings > Accessibility > Spoken Content > Voices*).

### Commands
```bash
# Record English Demo
./scripts/record_demo_en.sh

# Record Indonesian Demo
./scripts/record_demo_id.sh
```

---

## Pipeline Overview
1. **Setup**: Configures touch indicators and loads localized phrase presets and dual-language preferences into `shared_prefs`.
2. **UI Automation**: `scripts/run_demo_flow_*.sh` simulates gestures via `adb` and logs timestamped interaction events.
3. **Audio Synthesis**: `scripts/generate_audio*.py` synthesizes the two-tone Attention Chime (587.33 Hz / 880.00 Hz) and spoken TTS clips in both primary and secondary languages.
4. **Muxing**: `scripts/mux_audio*.py` aligns synthesized audio to logged timestamps and muxes video/audio via FFmpeg into the final MP4.
