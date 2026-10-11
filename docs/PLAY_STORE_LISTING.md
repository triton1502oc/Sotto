# Google Play Store Listing & Compliance Guide

### TL;DR
Ready-to-use metadata, descriptions, graphics specifications, and policy questionnaire answers for submitting Sotto to the Google Play Console.

---

## 1. Store Listing Metadata

### App Details
* **App Name:** `Sotto: Calm Meltdown Yes/No` *(27 / 30 characters; Google Play console title limit is 30 characters)*  
* **Short Description:**  
  `He uses cards for complex info. During meltdown I operate, he taps Yes/No.` *(74 / 80 characters)*  
* **Preview Video (YouTube URL):**  
  - **English:** `https://www.youtube.com/watch?v=Kd9qPu3F-Gg`  
  *(Note: Enter the standard `https://www.youtube.com/watch?v=...` format; Play Console may reject `/shorts/` URLs).*

### Full Description *(Plain Text & Markdown compliant)*
```text
When speech is hard, tap instead.

Built by a father in Singapore for his two autistic sons (22 & 15) who can speak but not always. During meltdown, operating a phone is too hard. So I operate Sotto on MY phone, they tap Yes/No/Wait to pinpoint the problem — issue resolved.

We use Sotto in two modes:

MODE 1 - DAILY (He holds his phone):
For complex information, noisy places, or new people. He opens Sotto himself and taps cards — Needs, Social, Care, General — or types in Quick-Speak Bar. One-tap speech, long-press for giant fullscreen text readable without speaking.

MODE 2 - MELTDOWN (You hold YOUR phone):
This is what stops meltdowns at home. You open Sotto on YOUR phone, tap partner mode, tap Flip 🔄 to show giant text to him, and he taps Yes / No / Wait / Repeat. No branching, no tree to navigate. That tap IS his voice right now.

Why this works:
• Direct Yes/No Pinpoint Cards: Huge 80dp+ buttons with haptics for shaky hands
• Safety Bystander Card: High-contrast amber hero for non-verbal episodes
• Two-Way Partner Mode: Talk HUD with dictation, preset direct questions, Read Aloud 🔊, 180° Flip 🔄
• Quick-Speak Bar: Spontaneous speech, fullscreen, dictation 🎤, 1-tap save
• Low-Stimulus UI: Dark-neutral Material 3, high contrast, zero cartoons
• Zero-Distraction Mode: No admin clutter during communication
• Dual-Language Offline: Display English, speak Indonesian/Malay/etc via ML Kit — 100% offline
• Private Pattern Log: After resolution, optional one-tap log Pain/Noise/Task/Emotion to find 30-day patterns locally
• QR Deck Sharing: Share phrase decks offline via QR SOTTO:v1:... between Android & iOS — zero cloud
• Backup v2: Selective export phrases, Why tree, Why log

100% offline core. Zero accounts, ads, trackers. Optional anonymous timing opt-in (zero text/speech ever collected). Open source.

Sotto is not a medical device and does not provide medical advice, diagnosis, or treatment. It is an assistive communication tool.

github.com/triton1502oc/Sotto | iOS version available
```

### What's New
```text
First production release — built from real family use. Direct Yes/No crisis cards now primary: You hold your phone during meltdown, he taps Yes/No/Wait to pinpoint and resolve. Why Finder moved to post-crisis explorer. Added QR offline sharing between Android & iOS.
```

---

## 2. Graphic Asset Requirements

| Asset | Dimensions | Format | Status / Location |
|---|---|---|---|
| **App Icon** | 512 x 512 px | 32-bit PNG (with alpha) | ✅ Ready: [`play_store_icon_512.png`](../assets/play_store_icon_512.png) |
| **Feature Graphic** | 1024 x 500 px | 24-bit PNG / JPEG | ✅ Ready: [`play_store_feature_graphic.png`](../assets/play_store_feature_graphic.png) |
| **Phone Screenshots** | Min 2 screenshots (Recommended 8) | JPEG or 24-bit PNG | 8 Planned / In-Progress (see below) |
| **Preview Video** | YouTube URL | YouTube link (`watch?v=`) | ✅ Ready: [English](https://www.youtube.com/watch?v=Kd9qPu3F-Gg) |

### Unified Screenshot Sequence (8 Screens)
1. **Dual mode split**: Teen self-using cards + Dad holding flipped phone with Yes/No
2. **Safety amber hero fullscreen**: Fullscreen high-contrast bystander card
3. **Huge Yes/No/Wait/Repeat buttons**: Direct 80dp+ binary crisis buttons with haptics
4. **Flip 180°**: Tabletop face-to-face partner mode
5. **Quick-Speak Bar**: Multi-line spontaneous input with mic dictation and speech
6. **Main board categories**: Low-stimulus Material 3 grid (Needs, Social, Care, General)
7. **Why Log 30-day trends**: Private local root-cause patterns
8. **Privacy + QR sharing**: 100% offline core, zero cloud, QR deck exchange

### Rebuilding Graphic Assets
To automatically regenerate and format all visual assets (`screenshot.png`, `screenshot_fullscreen.png`, `screenshot_partner_mode.png`, `screenshot_edit_phrase.png`, `screenshot_voice_settings.png`, and `play_store_feature_graphic.png`) from the latest app build:
```bash
./scripts/update_assets.sh
```

---

## 3. Data Safety Form (Google Play Console)

When completing the Google Play **Data Safety** questionnaire:

1. **Does your app collect or share any of the required user data types?**  
   👉 Select **Yes** (Only for optional, strictly opt-in anonymous interaction telemetry; core app remains zero-data).
   - **Data Types**:
     - *App activity > App interactions*: Generic UI interaction events, session duration brackets, and optional 3-question PMF survey responses. Zero phrase text or audio collected.
     - *Device or other IDs > App-assigned ID*: A random UUID generated locally upon telemetry opt-in to correlate anonymous sessions over time. Never linked to user identity, Google account, or hardware IDs (IMEI, Android ID, Advertising ID).
     - *App info and performance > Other app performance data*: Coarse time buckets (day-of-week and 4-hour window) and interaction outcome metrics.
   - **Collection vs Sharing**: Collected = **Yes**, Shared = **No** (Zero data shared with third parties or external commercial entities).
   - **Encrypted in transit**: **Yes** (HTTPS via TLS 1.3).
   - **Required or Optional**: **Optional** (Users must explicitly opt-in; default is OFF).
   - **Purpose**: **Analytics** (Diagnosing accessibility friction, evaluating crisis navigation effectiveness, and measuring product-market fit).
2. **Do you provide a way for users to request that their data be deleted?**  
   👉 Select **Yes** (Toggling the setting OFF in Voice & Language Settings immediately purges the app-assigned install ID and all queued events from local storage; uninstalling deletes all local app data).

---

## 4. App Content & Policy Ratings

* **Category:** Communication (or Tools / Health)
* **Tags:** AAC, Assistive, Speech, Autism, Speech Impairment, Unclear Speech, Dysarthria, Apraxia, Communication
* **Target Audience:** Select **13–15**, **16–17**, and **18+**.  
  *(Selecting 13+ prevents your app from being categorized under the "Designed for Families" COPPA framework, avoiding unnecessary pediatric compliance scrutiny).*
* **Content Rating (IARC):** Complete the questionnaire:
  * Violence: No
  * Sexual content: No
  * Offensive language: No
  * Controlled substances: No
  * Miscellaneous (user location, purchases, etc.): No  
  👉 Results in **Everyone / PEGI 3 / USK 0**.
* **Privacy Policy URL:** Link to your repository's policy or GitHub Pages URL:  
  `https://github.com/triton1502oc/Sotto/blob/main/PRIVACY_POLICY.md`

---

## 5. Closed Testing & Google Group (`sotto-testers`)

### Google Group Welcome Message
*Copy and paste directly into **Google Groups > Group settings > General > Welcome message**:*

```text
Welcome to Sotto Closed Testing!

To install the app and help us meet Google Play's 14-day testing requirement:

1. Join the test on Google Play:
   https://play.google.com/apps/testing/com.amh.sotto

2. Download & install Sotto from Google Play (via Android or the link above).

3. Keep the app installed on your device for at least 14 days and test basic features (tap cards to speak, quick-speak bar, offline speech).

• App overview & README: https://github.com/triton1502oc/Sotto#readme
• Feedback & Bug reports: Post here or open an issue at https://github.com/triton1502oc/Sotto/issues

Thank you for helping us launch Sotto!
```


