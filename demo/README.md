# Sotto Demo Videos

### TL;DR
This directory contains high-definition, synchronized demo videos showcasing Sotto's core features (Quick Speak, Emergency Hero Card, Contextual Categories, Fullscreen Presentation Mode, Card Reordering, and Voice Settings with Attention Chime) for both Indonesian and English.

---

## Videos

- **Indonesian Demo**: [`sotto_demo_id.mp4`](sotto_demo_id.mp4) (48s, 720×1600 @ 30fps, 4 Mbps H.264, 192 kbps AAC audio)
  - **YouTube**: [https://www.youtube.com/watch?v=UBBUd36im48](https://www.youtube.com/watch?v=UBBUd36im48) (Shorts: [https://youtube.com/shorts/UBBUd36im48](https://youtube.com/shorts/UBBUd36im48))
  - Spoken TTS Voice: `Damayanti` (id-ID)
  - Phrases: *"Tolong beri saya waktu."*, *"Saya tidak bisa bicara sekarang. Tolong baca layar saya."*, *"Halo, ini adalah suara bicara saya."*
- **English Demo**: [`sotto_demo_en.mp4`](sotto_demo_en.mp4) (48s, 720×1600 @ 30fps, 4 Mbps H.264, 192 kbps AAC audio)
  - **YouTube**: [https://www.youtube.com/watch?v=xpPbsiT7jks](https://www.youtube.com/watch?v=xpPbsiT7jks) (Shorts: [https://youtube.com/shorts/xpPbsiT7jks](https://youtube.com/shorts/xpPbsiT7jks))
  - Spoken TTS Voice: `Samantha` (en-US)
  - Phrases: *"I need a moment, please."*, *"I cannot speak right now. Please read my screen."*, *"Hello, this is my speaking voice."*

---

## How to Reproduce / Re-record

### Prerequisites

1. **Android Emulator / Device**:
   - Resolution: 1080×2400 (e.g. Pixel 8 / Medium Phone AVD).
   - Recommended AVD specs: RAM $\ge$ 4096 MB, VM Heap $\ge$ 512 MB to prevent system ANRs.
2. **FFmpeg**:
   ```bash
   brew install ffmpeg
   ```
3. **Android Tools (`adb`)**:
   Ensure `adb` is available in your `$PATH` or `$ANDROID_HOME/platform-tools/adb`.
4. **macOS TTS Voices** (used by Python audio generators):
   - English: `Samantha` (macOS default)
   - Indonesian: `Damayanti` (install via *System Settings > Accessibility > Spoken Content > Voices > Indonesian*)

---

### Recording Commands

#### 1. Record Indonesian Demo
```bash
./scripts/record_demo_id.sh
```

#### 2. Record English Demo
```bash
./scripts/record_demo_en.sh
```

---

## How the Pipeline Works

1. **Clean Environment Configuration**:
   - Enables touch pointers (`settings put system show_touches 1`).
   - Suppresses error popups and disables accessibility floating buttons to ensure a clean capture area.
   - Pushes appropriate locale and initial phrase sets into app `shared_prefs`.
2. **Synchronized UI Automation**:
   - [`scripts/run_demo_flow_id.sh`](../scripts/run_demo_flow_id.sh) / [`scripts/run_demo_flow_en.sh`](../scripts/run_demo_flow_en.sh) drives the UI using `adb shell input`.
   - Each speech or chime interaction logs a high-precision relative timestamp (e.g. `speak_waktu_1:5.890`) to an events file.
3. **Audio Synthesis**:
   - [`scripts/generate_audio_id.py`](../scripts/generate_audio_id.py) / [`scripts/generate_audio.py`](../scripts/generate_audio.py) replicates the exact two-tone Attention Chime synthesis from [`ChimePlayer.kt`](../app/src/main/java/com/sotto/aac/audio/ChimePlayer.kt) (D5 587.33 Hz + A5 880.00 Hz) and generates spoken TTS clips.
4. **Muxing**:
   - [`scripts/mux_audio_id.py`](../scripts/mux_audio_id.py) / [`scripts/mux_audio.py`](../scripts/mux_audio.py) places the audio clips along the timeline according to the logged event timestamps and muxes the video and audio tracks via FFmpeg into the final MP4.
