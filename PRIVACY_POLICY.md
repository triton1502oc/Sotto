# Privacy Policy for Sotto

### TL;DR
Sotto is built on a privacy-first, dignified foundation. Core speech synthesis and phrase management run 100% locally on your device with zero third-party tracking SDKs and zero accounts. Sotto never records, stores, or transmits your typed words, phrases, or speech recordings.

---

**Last Updated:** October 2, 2026

## 1. Overview
Sotto is an assistive communication (AAC) application designed for autistic individuals, speech-impaired individuals, non-speaking individuals, and anyone needing text-to-speech support. Your privacy, autonomy, and security are fundamental to our design philosophy.

## 2. Core Offline Operations (Zero Personal Data)
* **No Personal Accounts:** Sotto does not ask for, access, or store your name, email address, phone number, location, or any personal details.
* **No Phrase or Speech Collection:** All phrases, custom messages, and speech output are synthesized locally on your device. We never record, upload, or transmit what you type, read, or say.
* **Voice Input (Speech-to-Text):** When you use the microphone dictation feature, Sotto hands this request over to your device's default speech recognition engine. Sotto itself never collects or stores voice audio.
* **On-Device Translation:** If you use the Auto-Translate feature, language models run 100% locally on your device via Google ML Kit. Your text is never transmitted to external servers.
* **No Third-Party SDKs:** Sotto contains zero third-party advertising SDKs and zero commercial analytics tracking frameworks.

## 3. Optional Anonymous Usability Telemetry (Strict Opt-In)
To help us understand interaction difficulties and improve accessibility (e.g. detecting accidental rapid double-tapping or long hesitation times), Sotto includes an optional, privacy-preserving usability monitoring engine:

* **Strictly Opt-In (Default OFF):** Usability telemetry is disabled out-of-the-box. Users are presented with a one-time, low-stimulus prompt upon install or upgrade, and can toggle it ON or OFF anytime in **Voice & Language Settings**.
* **Zero Text or Audio Transmission:** Telemetry strictly collects only generic interaction metadata (e.g., button intent name, millisecond duration, outcome status, character length). It **NEVER** captures or transmits typed phrases, card text, or voice recordings.
* **Anonymous Ephemeral Session IDs:** Every app session generates a random 8-character identifier that is destroyed when the app exits. No device identifiers (IMEI, Android ID, or Advertising ID) are ever accessed or tracked.
* **Network & Storage Guardrails:** Usability events are queued in a small local sandboxed file strictly capped at 50 events (~15 KB). Events are batched and dispatched to a private developer endpoint strictly over unmetered Wi-Fi when the device has healthy battery.
* **Instant Purge on Opt-Out:** Toggling the setting OFF immediately deletes all locally queued usability events from the device and cancels pending background tasks.

## 4. Local Device Storage
Custom phrases, categories, and voice preferences are stored strictly in private, sandboxed app storage on your device (`SharedPreferences` and app internal files). This data is completely erased if you uninstall the app or clear app data.

## 5. Children's Privacy
Because Sotto does not collect personal data, it does not collect or solicit personal information from children or adults of any age.

## 6. Contact & Open Source
Sotto is free and open-source software licensed under GPLv3. If you have questions about our privacy practices, please contact us or inspect our source code:  
https://github.com/triton1502oc/Sotto
