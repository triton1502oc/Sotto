#!/usr/bin/env python3
import os
import struct
import subprocess
import wave

SAMPLE_RATE = 44100

def read_wav(filename):
    with wave.open(filename, 'rb') as wav_file:
        n_channels = wav_file.getnchannels()
        sampwidth = wav_file.getsampwidth()
        framerate = wav_file.getframerate()
        n_frames = wav_file.getnframes()
        data = wav_file.readframes(n_frames)
        samples = struct.unpack('<' + 'h' * (n_frames * n_channels), data)
        if n_channels == 2:
            samples = samples[::2]
        return list(samples)

def write_wav(filename, samples):
    with wave.open(filename, 'wb') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        data = struct.pack('<' + 'h' * len(samples), *samples)
        wav_file.writeframes(data)

def main():
    events_file = 'demo/events.txt'
    if not os.path.exists(events_file):
        print(f"Events file {events_file} not found.")
        return

    events = {}
    with open(events_file, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or ':' not in line:
                continue
            name, t_str = line.split(':', 1)
            events[name] = float(t_str)

    total_duration = events.get('end', 52.0) + 1.0
    total_samples = int(total_duration * SAMPLE_RATE)
    timeline = [0] * total_samples

    mapping = {
        'speak_moment_1': 'demo/audio_scratch/moment.wav',
        'speak_moment_2': 'demo/audio_scratch/moment.wav',
        'speak_emergency_en': 'demo/audio_scratch/emergency_en.wav',
        'speak_emergency_id': 'demo/audio_scratch/emergency_id.wav',
        'speak_time_en': 'demo/audio_scratch/time_en.wav',
        'speak_time_id': 'demo/audio_scratch/time_id.wav',
        'test_voice': 'demo/audio_scratch/chime_test_voice_en.wav'
    }

    for event_name, wav_file in mapping.items():
        if event_name in events and os.path.exists(wav_file):
            t_sec = events[event_name]
            samples = read_wav(wav_file)
            start_idx = int(t_sec * SAMPLE_RATE)
            for i, s in enumerate(samples):
                idx = start_idx + i
                if idx < total_samples:
                    mixed = timeline[idx] + s
                    timeline[idx] = max(-32768, min(32767, mixed))
            print(f"Placed {wav_file} at {t_sec:.2f}s")

    out_wav = 'demo/sotto_soundtrack.wav'
    write_wav(out_wav, timeline)
    print(f"Saved synchronized audio to {out_wav}")

    # Mux using ffmpeg
    ffmpeg_bin = '/opt/homebrew/bin/ffmpeg'
    if not os.path.exists(ffmpeg_bin):
        ffmpeg_bin = 'ffmpeg'

    in_video = 'demo/sotto_demo_en_video.mp4'
    out_video = 'demo/sotto_demo_en.mp4'
    cmd = [
        ffmpeg_bin, '-y',
        '-i', in_video,
        '-i', out_wav,
        '-c:v', 'copy',
        '-c:a', 'aac',
        '-b:a', '192k',
        '-shortest',
        out_video
    ]
    print("Running mux command:", " ".join(cmd))
    subprocess.run(cmd, check=True)
    print(f"Muxed video saved to {out_video}")

if __name__ == '__main__':
    main()
