# Sotto Demo Videos

### TL;DR
High-definition demo videos and reproduction scripts showcasing Sotto's core features in English and Indonesian.

---

## Demo Videos

| Video | Language | Voice | Watch | File |
|---|---|---|---|---|
| **English Demo** | `en-US` | Samantha | [YouTube](https://www.youtube.com/watch?v=xpPbsiT7jks) | [`sotto_demo_en.mp4`](sotto_demo_en.mp4) |
| **Indonesian Demo** | `id-ID` | Damayanti | [YouTube](https://www.youtube.com/watch?v=UBBUd36im48) | [`sotto_demo_id.mp4`](sotto_demo_id.mp4) |

*Specs: 48s, 720×1600 @ 30fps, H.264 / AAC.*

---

## Recording & Reproduction

### Prerequisites
- **Android Emulator/Device**: 1080×2400 (e.g. Pixel 8), $\ge$ 4 GB RAM.
- **Tools**: `ffmpeg` (`brew install ffmpeg`), `adb` in `$PATH`.
- **macOS TTS Voices**: `Samantha` (default) and `Damayanti` (*System Settings > Accessibility > Spoken Content > Voices*).

### Commands
```bash
# Record English Demo
./scripts/record_demo_en.sh

# Record Indonesian Demo
./scripts/record_demo_id.sh
```

---

## Pipeline Overview
1. **Setup**: Configures touch indicators and loads localized phrase presets into `shared_prefs`.
2. **UI Automation**: `scripts/run_demo_flow_*.sh` simulates gestures via `adb` and logs timestamped interaction events.
3. **Audio Synthesis**: `scripts/generate_audio*.py` synthesizes the two-tone Attention Chime (587.33 Hz / 880.00 Hz) and spoken TTS clips.
4. **Muxing**: `scripts/mux_audio*.py` aligns synthesized audio to logged timestamps and muxes video/audio via FFmpeg.

