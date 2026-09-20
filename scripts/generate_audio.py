#!/usr/bin/env python3
import math
import os
import struct
import subprocess
import wave

SAMPLE_RATE = 44100

def generate_chime_samples():
    """Exact replica of ChimePlayer.kt synthesis."""
    duration1 = 0.09
    duration2 = 0.16
    total_duration = duration1 + duration2
    num_samples1 = int(duration1 * SAMPLE_RATE)
    total_samples = int(total_duration * SAMPLE_RATE)
    num_samples2 = total_samples - num_samples1

    samples = []
    freq1 = 587.33
    freq2 = 880.00

    # Note 1 (D5)
    for i in range(num_samples1):
        t = i / SAMPLE_RATE
        angle = 2.0 * math.pi * freq1 * t
        attack = min(i / (0.01 * SAMPLE_RATE), 1.0)
        decay = 1.0 - (i / num_samples1) * 0.3
        envelope = attack * decay
        val = int(math.sin(angle) * envelope * 32767 * 0.75)
        samples.append(max(-32768, min(32767, val)))

    # Note 2 (A5)
    for i in range(num_samples2):
        t = i / SAMPLE_RATE
        angle = 2.0 * math.pi * freq2 * t
        attack = min(i / (0.008 * SAMPLE_RATE), 1.0)
        decay = math.exp(-4.5 * (i / num_samples2))
        envelope = attack * decay
        val = int((math.sin(angle) * 0.8 + math.sin(2.0 * angle) * 0.2) * envelope * 32767 * 0.75)
        samples.append(max(-32768, min(32767, val)))

    return samples

def write_wav(filename, samples):
    with wave.open(filename, 'wb') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        data = struct.pack('<' + 'h' * len(samples), *samples)
        wav_file.writeframes(data)

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

def tts_to_wav(text, out_wav, voice='Samantha'):
    aiff_file = out_wav + '.aiff'
    subprocess.run(['say', '-v', voice, '-o', aiff_file, text], check=True)
    subprocess.run(['afconvert', '-f', 'WAVE', '-d', 'LEI16@44100', aiff_file, out_wav], check=True)
    if os.path.exists(aiff_file):
        os.remove(aiff_file)

if __name__ == '__main__':
    os.makedirs('demo/audio_scratch', exist_ok=True)
    chime_samples = generate_chime_samples()
    write_wav('demo/audio_scratch/chime.wav', chime_samples)

    # English clips (Samantha)
    tts_to_wav("I need a moment, please.", 'demo/audio_scratch/moment.wav', voice='Samantha')
    tts_to_wav("I cannot speak right now. Please read my screen.", 'demo/audio_scratch/emergency_en.wav', voice='Samantha')
    tts_to_wav("Please give me time.", 'demo/audio_scratch/time_en.wav', voice='Samantha')
    tts_to_wav("Hello, this is my speaking voice.", 'demo/audio_scratch/test_voice_en.wav', voice='Samantha')

    # Indonesian clips for dual-language showcase (Damayanti)
    tts_to_wav("Saya tidak bisa bicara sekarang. Tolong baca layar saya.", 'demo/audio_scratch/emergency_id.wav', voice='Damayanti')
    tts_to_wav("Tolong beri saya waktu.", 'demo/audio_scratch/time_id.wav', voice='Damayanti')

    # Attention chime + English voice test
    chime_test = chime_samples + [0] * int(0.28 * SAMPLE_RATE) + read_wav('demo/audio_scratch/test_voice_en.wav')
    write_wav('demo/audio_scratch/chime_test_voice_en.wav', chime_test)
    print("English and dual-language audio clips generated successfully.")
