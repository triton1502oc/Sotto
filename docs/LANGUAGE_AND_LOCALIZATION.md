# Language & Localization

### TL;DR
- **First Launch**: 9 default cards are seeded in English or Indonesian based on device locale and saved to local storage (`SharedPreferences`).
- **Changing App Language (Settings)**: Changes UI labels and default TTS voice. Existing cards **do not change** to protect user edits, but Sotto auto-detects their language so TTS still pronounces them correctly.
- **Dual-Language Switcher (`EN` ⇄ `ID` on Home)**: Tapping cards while `ID` is active translates English cards into Indonesian offline (ML Kit) and speaks Indonesian.

---

## 1. Default Cards (First Launch)

On first run, [`SharedPreferencesPhraseRepository`](../app/src/main/java/com/amh/sotto/data/PhraseRepository.kt) saves 9 default cards to local storage from XML:

| Category | English (`values/strings.xml`) | Indonesian (`values-in/strings.xml`) |
| :--- | :--- | :--- |
| **Emergency** (🚨 full width) | "I cannot speak right now. Please read my screen." | "Saya tidak bisa bicara sekarang. Tolong baca layar saya." |
| **Needs** | 1. "I need a quiet space."<br>2. "Please give me time."<br>3. "I need to leave now." | 1. "Saya butuh tempat tenang."<br>2. "Tolong beri saya waktu."<br>3. "Saya harus pergi sekarang." |
| **Social** | 1. "Yes, please."<br>2. "No, thank you."<br>3. "Thank you." | 1. "Ya, silakan."<br>2. "Tidak, terima kasih."<br>3. "Terima kasih." |
| **General** | 1. "Hello."<br>2. "Please repeat that." | 1. "Halo."<br>2. "Tolong ulangi." |

> Existing cards are never overwritten on app updates or locale changes.

---

## 2. Switching Language

There are two distinct language features in Sotto:

### A. App Language (Settings → App Language)
- **UI Chrome**: All app text (buttons, category tabs, hints) switches to Indonesian.
- **Existing Cards**: Remain in their original language (persisted in `sotto_prefs`).
- **TTS Engine**: Default voice switches to Indonesian, but [`LocaleHelper.detectLocale()`](../app/src/main/java/com/amh/sotto/util/LocaleHelper.kt) detects English words on existing cards and still speaks them with an English voice.

### B. Dual-Language Switcher (Top Bar `[ EN | ID ]`)
*Enabled via Settings → "Show language switcher on home"*.
- **`EN` Active**: Cards speak in English.
- **`ID` Active**: Tapping an English card translates it on-the-fly to Indonesian offline via Google ML Kit ([`TranslationHelper`](../app/src/main/java/com/amh/sotto/util/TranslationHelper.kt)), displays a `🗣️` subtitle, and speaks in Indonesian.
- **Fullscreen**: Long-press any card to choose **Speak (English)** or **Speak (Bahasa Indonesia)**.
- **Quick-Speak Bar**: Translates typed input into the selected language before speaking.

### C. Offline Readiness & Voice Data
- **Translation Models**: Downloaded once (~30 MB) and kept 100% offline via ML Kit.
- **TTS Voice Packs**: Settings detects if high-quality offline voice data is installed for the target language, providing a 1-tap shortcut to system TTS download settings if missing.

---

## 3. Key Files

- [`LocaleHelper.kt`](../app/src/main/java/com/amh/sotto/util/LocaleHelper.kt): Locale management and keyword-based language detection.
- [`PhraseRepository.kt`](../app/src/main/java/com/amh/sotto/data/PhraseRepository.kt): Persistent JSON storage in `SharedPreferences`.
- [`TranslationHelper.kt`](../app/src/main/java/com/amh/sotto/util/TranslationHelper.kt): Offline ML Kit translation.
- [`MainActivity.kt`](../app/src/main/java/com/amh/sotto/MainActivity.kt): TTS routing, top bar toggle, and card actions.
