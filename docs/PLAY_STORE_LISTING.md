# Google Play Store Listing & Compliance Guide

### TL;DR
Ready-to-use metadata, descriptions, graphics specifications, and policy questionnaire answers for submitting Sotto to the Google Play Console.

---

## 1. Store Listing Metadata

### App Details
* **App Name:** `Sotto` *(5 / 30 characters)*
* **Short Description:**  
  `Calm yes/no tool for caregivers to reach autistic teens & adults in meltdowns.` *(78 / 80 characters)*
* **Preview Video (YouTube URL):**
  - **Default / English:** `https://www.youtube.com/watch?v=UKZu5ZsdORg`
  - **Indonesian (`id-ID`):** `https://www.youtube.com/watch?v=4CvpJkjaz-k`  
  *(Note: Enter the standard `https://www.youtube.com/watch?v=...` format; Play Console may reject `/shorts/` URLs).*

### Full Description *(Markdown & Plain Text compliant, ~2,800 / 4,000 characters)*
```text
Sotto is a calm yes/no crisis navigation tool for caregivers, parents, and therapists to connect with autistic teens and adults during sensory overload, meltdowns, and verbal shutdowns — paired with a dignified, low-stimulation AAC speech board for daily self-expression and unclear speech support.

Created by a father of two autistic sons (ages 22 and 15), Sotto is built around a simple reality: during a meltdown or verbal shutdown, processing complex questions or speaking is exhausting. Sotto bridges this divide with gentle binary choices, speech clarity support, and zero open-ended demands.

FOR CAREGIVERS, FAMILIES & THERAPISTS:

• Why Finder (Guided Crisis Navigation)
When someone is experiencing sensory overload or verbal paralysis, open-ended questions like "What's wrong?" can worsen distress. Why Finder provides a gentle, step-by-step yes/no branching tree (covering Physical, Sensory, Emotional, and Routine triggers) with giant buttons to calmly identify the root cause.

• Private On-Device Why Log & Pattern Trends
Resolved crisis sessions are automatically logged 100% locally on your device. Review 30-day patterns to identify frequent environmental triggers, recurring peak hours, and actionable interventions. Never shared or uploaded to the cloud.

• Customizable Why Tree & Safe Sharing
Adapt questions and areas to your loved one's specific triggers using voice dictation and reordering. Easily export and share customized trees with teachers or speech therapists while keeping private crisis logs strictly excluded.

• Partner Mode & 180° Screen Flip (Guided & Talk)
Switch effortlessly between Guided Mode (Why Finder crisis inquiry) and Talk Mode (speech-to-text dictation with preset questions). Tap 180° Flip and place the phone on a table: your conversation partner taps one of 4 large, dignified responses (Yes, No, Repeat, Wait, or Not Sure/Stop in Guided mode) with instant voice confirmation.

FOR NON-SPEAKING & UNCLEAR VOICES (DIGNIFIED SELF-EXPRESSION):

• Mature, Dignified Voice for Unclear or Fatigued Speech
Whether completely non-verbal, navigating an autistic shutdown, or speaking with quiet, slurred, or dysarthric speech that listeners struggle to understand, Sotto provides a crisp, adult text-to-speech voice and high-contrast cards—free of childish cartoons and patronizing symbols.

• Emergency Bystander Hero Card
A prominent high-contrast banner for non-verbal episodes. Show fullscreen directly to bystanders, first responders, or medical personnel.

• Quick-Speak Bar & Voice Dictation
Type spontaneous thoughts with auto-expanding multi-line text, one-tap speech, voice dictation, and one-touch card creation.

• High-Visibility Giant Fullscreen
Display any phrase across the entire display in giant text to silently communicate in loud or crowded environments.

• Contextual Categories & Custom Boards
Filter phrases by situation (Emergency, Needs, Social, Care, General) or create custom categories.

• Dual-Language Speech & On-Device Auto-Translate
Display cards in one language while speaking in another (e.g. English text read aloud in Indonesian) using 100% offline on-device translation.

• 100% Offline Core & Privacy Guarantee
Zero accounts. Zero ads. Zero cloud speech recording. Your phrases and private logs never leave your phone. Optional, strictly opt-in anonymous usability timing to help diagnose accessibility friction (zero text or speech collected).
```

---

## 2. Graphic Asset Requirements

| Asset | Dimensions | Format | Status / Location |
|---|---|---|---|
| **App Icon** | 512 x 512 px | 32-bit PNG (with alpha) | ✅ Ready: [`play_store_icon_512.png`](../assets/play_store_icon_512.png) |
| **Feature Graphic** | 1024 x 500 px | 24-bit PNG / JPEG | ✅ Ready: [`play_store_feature_graphic.png`](../assets/play_store_feature_graphic.png) |
| **Phone Screenshots** | Min 2 screenshots | JPEG or 24-bit PNG | ✅ 4 Ready: [`screenshot.png`](../assets/screenshot.png) (Main phrase view), [`screenshot_fullscreen.png`](../assets/screenshot_fullscreen.png) (Emergency Bystander & Fullscreen Mode), [`screenshot_edit_phrase.png`](../assets/screenshot_edit_phrase.png) (Phrase Customization & Translation), [`screenshot_voice_settings.png`](../assets/screenshot_voice_settings.png) (Voice & Language Settings) |
| **Preview Video** | YouTube URL | YouTube link (`watch?v=`) | ✅ Ready: [English](https://www.youtube.com/watch?v=UKZu5ZsdORg) / [Indonesian](https://www.youtube.com/watch?v=4CvpJkjaz-k) |

### Rebuilding Graphic Assets
To automatically regenerate and format all visual assets (`screenshot.png`, `screenshot_fullscreen.png`, `screenshot_edit_phrase.png`, `screenshot_voice_settings.png`, and `play_store_feature_graphic.png`) from the latest app build:
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


