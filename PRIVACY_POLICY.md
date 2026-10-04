# Privacy Policy for Sotto

### TL;DR
Sotto is built on a privacy-first, dignified foundation. Core speech synthesis, phrase management, Why Finder sessions, and private crisis logs run 100% locally on your device with zero third-party tracking SDKs and zero user accounts. Sotto never records, stores, or transmits your typed words, spoken phrases, audio recordings, or private Why logs.

---

**Last Updated:** October 4, 2026

## 1. Overview
Sotto is an assistive communication (AAC) and calm crisis navigation application designed for autistic individuals, caregivers, speech-impaired individuals, non-speaking individuals, and anyone needing text-to-speech support. Your privacy, autonomy, and emotional security are fundamental to our design philosophy.

## 2. Core Offline Operations (Zero Personal Data)
* **No Personal Accounts:** Sotto does not ask for, access, or store your name, email address, phone number, location, or any personal details.
* **No Phrase or Speech Collection:** All phrases, custom messages, and speech output are synthesized locally on your device using Android's offline Text-to-Speech engine. We never record, upload, or transmit what you type, read, or say.
* **Why Finder & Private Why Log:** Guided Why Finder sessions and crisis log history are stored strictly in private, on-device app storage. Why logs are never uploaded or transmitted off the device. When creating backups, Why Log history is excluded by default and requires explicit opt-in with a confirmation warning.
* **Voice Input (Speech-to-Text):** When you use the microphone dictation feature, Sotto hands this request over to your device's default speech recognition engine. Sotto itself never collects or stores voice audio.
* **On-Device Translation:** If you use the Auto-Translate feature, language models run 100% locally on your device via Google ML Kit. Your text is never transmitted to external servers.
* **No Third-Party SDKs:** Sotto contains zero third-party advertising SDKs and zero commercial analytics tracking frameworks.

## 3. Optional Anonymous Usability Telemetry (Strict Opt-In)
To help us understand interaction difficulties, optimize crisis navigation, and evaluate product-market fit without compromising privacy, Sotto includes an optional, privacy-preserving usability monitoring engine:

* **Strictly Opt-In (Default OFF):** Usability telemetry is disabled out-of-the-box. Users are presented with a one-time, low-stimulus prompt upon install or upgrade, and can toggle it ON or OFF anytime in **Voice & Language Settings**.
* **Zero Text, Speech, or Phrase Hash Collection:** Telemetry strictly collects only generic interaction metadata. In v1.6.0+, phrase hashes are completely dropped — phrases are recorded solely by character length bracket or category. Verbatim words, card text, audio, and Why Log contents are **NEVER** captured or transmitted.
* **App-Assigned Random Install ID:** When opt-in is enabled, a random UUID is generated locally on your device to correlate usage patterns over time. Sotto never accesses or tracks hardware identifiers (IMEI, Android ID, MAC address, or Google Advertising ID).
* **Coarse Temporal Bucketing:** Events record only the day of the week (`Mon`..`Sun`) and a 4-hour window (`00-04`, `04-08`, etc.). Exact timestamps and absolute calendar dates are never transmitted.
* **User Role Context:** Users may optionally choose a general role (`Caregiver`, `Myself (Autistic Teen / Adult)`, `Professional / Therapist / Teacher`, or `Unset`) to help us understand who benefits most from Sotto.
* **Event Types Collected:**
  - App opens (`app_open`) and active day counts.
  - Interaction events (phrase card taps by character length bracket, quick speak taps, category switches).
  - Why Finder crisis navigation (session started, tree steps traversed, resolution vs abortion status, elapsed duration bracket).
  - Two-Way receptive mode (mode opened, recognized question length, spoken response, completion vs dismissal).
  - Feature activations (emergency mode triggers, backup export/import indicators, voice setting adjustments).
  - 14-Day In-App Survey: Opted-in users who have used Sotto for at least 14 days across 5 active days receive a one-time 3-question survey (Sean Ellis disappointment score, primary benefit used, confirmed role).
* **Transparent Payload Preview:** Users can inspect the exact telemetry payload queued on their device at any time from Voice & Language Settings via the **Preview Telemetry Payload** button.
* **Network & Storage Guardrails:** Usability events are queued locally in sandboxed app storage strictly capped at 500 events (~60 KB). Batches are dispatched over unmetered Wi-Fi only when the device battery is not low.
* **Instant Purge on Opt-Out:** Toggling the setting OFF immediately deletes the app-assigned install ID and all queued telemetry records from the device, and cancels pending background dispatches.

## 4. Local Device Storage
Custom phrases, categories, Why Finder trees, Why logs, and voice preferences are stored strictly in private, sandboxed app storage on your device (`SharedPreferences` and app internal files). This data is completely erased if you uninstall the app or clear app data.

## 5. Children's Privacy
Because Sotto does not collect personal data, it does not collect or solicit personal information from children or adults of any age.

## 6. Contact & Open Source
Sotto is free and open-source software licensed under GPLv3. If you have questions about our privacy practices, please contact us or inspect our source code:  
https://github.com/triton1502oc/Sotto
