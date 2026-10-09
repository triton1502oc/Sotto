#!/usr/bin/env python3
"""Build the Google Play preview soundtrack (demo/sotto_google_play_soundtrack.wav).

Narration uses Microsoft neural voices via `edge-tts` (needs internet at build time only;
only the marketing script below is sent, no user data). Install: `pip install edge-tts`.
"""
import math, os, shutil, struct, subprocess, sys, wave

SR = 44100
DURATION = 60.0
OUT = "demo/sotto_google_play_soundtrack.wav"
TMP = "/tmp/sotto_play_audio"
NARRATOR = ("en-US-ChristopherNeural", "-3%", "-4Hz")   # deep, resonant baritone
APP_VOICE = ("en-US-AndrewNeural", "+0%", "+0Hz")  # distinct "device" voice

# (start_sec, voice, text) — starts align with scene boundaries in render_google_play_video.py
CUES = [
    (0.8, NARRATOR, "When words become hard during sensory overload or verbal shutdown, Sotto is here."),
    (9.0, NARRATOR, "Never ask “What's wrong?”. Why Finder guides caregivers through gentle yes-or-no questions to uncover distress calmly."),
    (19.8, NARRATOR, "In Partner Mode, flip the screen and set the phone between you. They can answer across the table with a single tap."),
    (29.2, APP_VOICE, "Yes."),
    (31.6, NARRATOR, "Day to day, speak instantly with one tap, or show high-contrast safety cards directly to bystanders."),
    (39.6, APP_VOICE, "I need a moment, please."),
    (42.5, NARRATOR, "Sensory color themes for low-stimulus comfort. And zero data leaves your device: no accounts, no tracking, 100% offline."),
    (53.8, NARRATOR, "Sotto. A calm voice, when words are hard."),
]


def tts(voice, text, out_wav):
    name, rate, pitch = voice
    mp3 = out_wav + ".mp3"
    exe = shutil.which("edge-tts") or ("/tmp/ttsvenv/bin/edge-tts" if os.path.exists("/tmp/ttsvenv/bin/edge-tts") else None) or os.path.join(os.path.dirname(sys.executable), "edge-tts")
    subprocess.run([exe, "--voice", name, f"--rate={rate}", f"--pitch={pitch}",
                    "--text", text, "--write-media", mp3], check=True, capture_output=True)
    subprocess.run(["ffmpeg", "-y", "-i", mp3, "-ac", "1", "-ar", str(SR), out_wav],
                   check=True, capture_output=True)


def read(path):
    with wave.open(path) as w:
        return list(struct.unpack(f"<{w.getnframes()}h", w.readframes(w.getnframes())))


def chime():
    """Replica of ChimePlayer.kt (D5 587.33 Hz + A5 880 Hz)."""
    n1, n2 = int(0.09 * SR), int(0.16 * SR)
    a = [math.sin(2 * math.pi * 587.33 * i / SR) * min(i / (0.01 * SR), 1) * (1 - 0.3 * i / n1) for i in range(n1)]
    b = [(0.8 * math.sin(2 * math.pi * 880 * i / SR) + 0.2 * math.sin(4 * math.pi * 880 * i / SR))
         * min(i / (0.008 * SR), 1) * math.exp(-4.5 * i / n2) for i in range(n2)]
    return [s * 0.5 * 32767 for s in a + b]


def ambient(n):
    """Soft pad: Dm9 → Bbmaj7 → Fmaj9 → Cadd9 with slow crossfades."""
    chords = [[146.83, 174.61, 220.0, 261.63], [116.54, 174.61, 220.0, 293.66],
              [87.31, 130.81, 220.0, 329.63], [130.81, 196.0, 293.66, 329.63]]
    seg, fade, out = n // 4, int(2.5 * SR), [0.0] * n
    for c, freqs in enumerate(chords):
        for i in range(seg):
            env = min(1, i / fade, (seg - i) / fade)
            t = i / SR
            out[c * seg + i] = env * sum(math.sin(2 * math.pi * f * t) for f in freqs) * 0.012 * 32767
    return out


def main():
    os.makedirs(TMP, exist_ok=True)
    n = int(DURATION * SR)
    mix = ambient(n)
    for k, (start, voice, text) in enumerate(CUES):
        path = f"{TMP}/cue{k}.wav"
        tts(voice, text, path)
        clip = read(path)
        end = start + len(clip) / SR
        print(f"cue{k}: {start:5.1f}s → {end:5.1f}s  {text[:50]}")
        s0 = int(start * SR)
        for i, v in enumerate(clip[: n - s0]):
            mix[s0 + i] += v * 0.95
    for i, v in enumerate(chime()):  # chime precedes the in-app phrase, like the real app
        mix[int(39.0 * SR) + i] += v
    
    # 2.0s master fadeout
    fade_len = int(2.0 * SR)
    for i in range(fade_len):
        mix[n - 1 - i] *= (i / fade_len)
        
    peak = max(abs(v) for v in mix)
    gain = min(1.0, 30000 / peak)
    with wave.open(OUT, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<hh", int(v * gain), int(v * gain)) for v in mix))
    print(f"Wrote {OUT} ({DURATION}s)")


if __name__ == "__main__":
    main()
