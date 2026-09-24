# Google Play Store Listing & Compliance Guide

### TL;DR
Ready-to-use metadata, descriptions, graphics specifications, and policy questionnaire answers for submitting Sotto to the Google Play Console.

---

## 1. Store Listing Metadata

### App Details
* **App Name:** `Sotto` *(5 / 30 characters)*
* **Short Description:**  
  `Dignified AAC speech communication for autistic & speech-impaired users.` *(71 / 80 characters)*
* **Preview Video (YouTube URL):**
  - **Default / English:** `https://www.youtube.com/watch?v=xpPbsiT7jks`
  - **Indonesian (`id-ID`):** `https://www.youtube.com/watch?v=UBBUd36im48`  
  *(Note: Enter the standard `https://www.youtube.com/watch?v=...` format; Play Console may reject `/shorts/` URLs).*

### Full Description *(Markdown & Plain Text compliant, ~1,800 / 4,000 characters)*
```text
Sotto (meaning "under one's breath" or a soft voice) is an assistive communication (AAC) app designed for autistic teens, adults, speech-impaired individuals, and non-speaking users who need clear, dignified, and low-stimulation text-to-speech support.

Unlike typical AAC tools designed with pediatric cartoon graphics and cluttered menus, Sotto provides a calm, mature, and predictable environment built specifically for adult communication needs.

KEY FEATURES:
 
• Modern Low-Stimulus UI
Dark-neutral Material 3 theme with high contrast and zero unnecessary animations or visual distractions.
 
• Zero-Distraction Communication Mode
In normal mode, all administrative buttons (add, delete, reorder) are completely hidden. Tap any phrase card to speak immediately using Android's offline Text-to-Speech engine.
 
• Emergency Bystander Mode
A dedicated hero card for verbal shutdowns. Long-press or tap fullscreen to display a high-contrast emergency banner explaining non-verbal episodes directly to bystanders, medical staff, or first responders.

• Quick-Speak Bar
Speak spontaneous, one-off thoughts instantly with multi-line readability. Includes instant fullscreen display, voice dictation, and one-tap card saving.

• Contextual Categories
Organize and filter phrases by situation (Emergency, Needs, Social, General) using clean, low-stimulus filter chips.

• High-Visibility Fullscreen Mode
In noisy, crowded, or overwhelming environments, view any phrase in giant, high-contrast text across your entire screen to silently show your phone to others.

• Attention Chime & Haptics
Optionally sound a gentle chime and vibration before speaking to politely capture listener attention in noisy spaces.
 
• Configurable Voice & Speed
Tailor your speech output to your comfort. Adjust speech rate (speed) and pitch with live preview.
 
• Dual-Language Speech & On-Device Auto-Translate
Display cards in one language while speaking in another (e.g., English card text read aloud in Indonesian). Includes one-tap on-device auto-translation with zero cloud text transmission and dual-language speak controls in fullscreen view.

• 100% Offline & Private
Zero accounts. Zero tracking. Zero cloud uploads. Your phrases and settings stay entirely on your local device.
```

---

## 2. Graphic Asset Requirements

| Asset | Dimensions | Format | Status / Location |
|---|---|---|---|
| **App Icon** | 512 x 512 px | 32-bit PNG (with alpha) | ✅ Ready: [`play_store_icon_512.png`](../assets/play_store_icon_512.png) |
| **Feature Graphic** | 1024 x 500 px | 24-bit PNG / JPEG | ✅ Ready: [`play_store_feature_graphic.png`](../assets/play_store_feature_graphic.png) |
| **Phone Screenshots** | Min 2 screenshots | JPEG or 24-bit PNG | ✅ 4 Ready: [`screenshot.png`](../assets/screenshot.png) (Main phrase view), [`screenshot_fullscreen.png`](../assets/screenshot_fullscreen.png) (Emergency Bystander & Fullscreen Mode), [`screenshot_edit_phrase.png`](../assets/screenshot_edit_phrase.png) (Phrase Customization & Translation), [`screenshot_voice_settings.png`](../assets/screenshot_voice_settings.png) (Voice & Language Settings) |
| **Preview Video** | YouTube URL | YouTube link (`watch?v=`) | ✅ Ready: [English](https://www.youtube.com/watch?v=xpPbsiT7jks) / [Indonesian](https://www.youtube.com/watch?v=UBBUd36im48) |

### Rebuilding Graphic Assets
To automatically regenerate and format all visual assets (`screenshot.png`, `screenshot_fullscreen.png`, `screenshot_edit_phrase.png`, `screenshot_voice_settings.png`, and `play_store_feature_graphic.png`) from the latest app build:
```bash
./scripts/update_assets.sh
```

---

## 3. Data Safety Form (Google Play Console)

When completing the Google Play **Data Safety** questionnaire:

1. **Does your app collect or share any of the required user data types?**  
   👉 Select **No**.
2. **Is all of the user data collected by your app encrypted in transit?**  
   👉 Select **N/A** (no data collected).
3. **Do you provide a way for users to request that their data be deleted?**  
   👉 Select **Yes** (Uninstalling the app or clearing app storage deletes all local data immediately).

---

## 4. App Content & Policy Ratings

* **Category:** Communication (or Tools / Health)
* **Tags:** AAC, Assistive, Speech, Autism, Speech Impairment, Communication
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


