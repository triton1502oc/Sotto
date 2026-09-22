package com.amh.sotto.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "sotto_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANG_AUTO = "auto"
    const val LANG_SYSTEM = "system"
    const val LANG_ENGLISH = "en"
    const val LANG_INDONESIAN = "id"



    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (languageCode == LANG_SYSTEM) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            }
        }
    }

    private val iso3ToIso1Map: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        for (iso2 in Locale.getISOLanguages()) {
            try {
                val iso3 = Locale(iso2).isO3Language.lowercase(Locale.ROOT)
                map[iso3] = iso2
            } catch (_: Exception) {}
        }
        map
    }

    fun getLocaleForLanguage(languageCode: String): Locale {
        val normalized = if (languageCode.length == 3) iso3ToIso1Map[languageCode.lowercase(Locale.ROOT)] ?: languageCode else languageCode
        return when (normalized) {
            LANG_SYSTEM -> Locale.getDefault()
            LANG_INDONESIAN, "in" -> Locale.forLanguageTag("id-ID")
            LANG_ENGLISH -> Locale.US
            "es" -> Locale.forLanguageTag("es-ES")
            "fr" -> Locale.FRANCE
            "de" -> Locale.GERMANY
            "it" -> Locale.ITALY
            "pt" -> Locale.forLanguageTag("pt-BR")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPAN
            "ko" -> Locale.KOREA
            "ru" -> Locale.forLanguageTag("ru-RU")
            "ar" -> Locale.forLanguageTag("ar-SA")
            "hi" -> Locale.forLanguageTag("hi-IN")
            "tr" -> Locale.forLanguageTag("tr-TR")
            "vi" -> Locale.forLanguageTag("vi-VN")
            "th" -> Locale.forLanguageTag("th-TH")
            "nl" -> Locale.forLanguageTag("nl-NL")
            "pl" -> Locale.forLanguageTag("pl-PL")
            "uk" -> Locale.forLanguageTag("uk-UA")
            "sv" -> Locale.forLanguageTag("sv-SE")
            "da" -> Locale.forLanguageTag("da-DK")
            "fi" -> Locale.forLanguageTag("fi-FI")
            "no" -> Locale.forLanguageTag("nb-NO")
            "el" -> Locale.forLanguageTag("el-GR")
            "he", "iw" -> Locale.forLanguageTag("he-IL")
            "ms" -> Locale.forLanguageTag("ms-MY")
            "fil", "tl" -> Locale.forLanguageTag("fil-PH")
            "bn" -> Locale.forLanguageTag("bn-BD")
            "ta" -> Locale.forLanguageTag("ta-IN")
            "te" -> Locale.forLanguageTag("te-IN")
            "mr" -> Locale.forLanguageTag("mr-IN")
            "ur" -> Locale.forLanguageTag("ur-PK")
            "cs" -> Locale.forLanguageTag("cs-CZ")
            "ro" -> Locale.forLanguageTag("ro-RO")
            "hu" -> Locale.forLanguageTag("hu-HU")
            "fa" -> Locale.forLanguageTag("fa-IR")
            "sw" -> Locale.forLanguageTag("sw-KE")
            else -> Locale.forLanguageTag(normalized)
        }
    }

    fun getLanguageDisplayName(code: String): String {
        if (code == LANG_SYSTEM) return "System Default"
        val locale = getLocaleForLanguage(code)
        val nativeName = locale.getDisplayLanguage(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        val englishName = locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
        return if (nativeName.equals(englishName, ignoreCase = true) || englishName.isBlank()) {
            nativeName
        } else {
            "$nativeName ($englishName)"
        }
    }

    fun getAvailableLanguages(ttsLocales: Set<Locale>? = null): List<Pair<String, String>> {
        val languageCodes = linkedSetOf<String>()
        
        // Always include core Sotto languages (English & Indonesian)
        languageCodes.add("en")
        languageCodes.add("id")

        if (!ttsLocales.isNullOrEmpty()) {
            ttsLocales.forEach { locale ->
                val rawLang = locale.language.lowercase(Locale.ROOT)
                val lang = if (rawLang.length == 3) iso3ToIso1Map[rawLang] ?: rawLang else rawLang
                if (lang.isNotBlank()) {
                    languageCodes.add(lang)
                }
            }
        }

        val list = languageCodes.map { code ->
            code to getLanguageDisplayName(code)
        }.sortedBy { it.second }

        return listOf(LANG_SYSTEM to "System Default") + list
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getLanguage(context)
        if (languageCode == LANG_SYSTEM) {
            return context
        }

        val locale = getLocaleForLanguage(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getEffectiveLanguage(context: Context): String {
        val lang = getLanguage(context)
        return if (lang == LANG_SYSTEM) {
            val sysLang = Locale.getDefault().language
            if (sysLang == "in") LANG_INDONESIAN else sysLang
        } else {
            if (lang == "in") LANG_INDONESIAN else lang
        }
    }

    // Common Indonesian words used to detect if a phrase is Indonesian
    private val indonesianWords = hashSetOf(
        "saya", "aku", "kami", "kita", "kamu", "anda", "dia", "mereka",
        "ini", "itu", "tolong", "bisa", "dapat", "ya", "tidak", "bukan",
        "terima", "kasih", "butuh", "tempat", "tenang", "sebentar",
        "merespons", "merasa", "kewalahan", "tuliskan", "kirim", "pesan",
        "ada", "di", "ke", "dari", "yang", "dan", "untuk", "dengan", "mau",
        "ingin", "makan", "minum", "sakit", "halo", "selamat", "pagi", "siang",
        "sore", "malam", "apa", "siapa", "mengapa", "kenapa", "bagaimana",
        "kapan", "mana", "sudah", "belum", "lagi", "bantu", "permisi", "air"
    )

    // Common English words used to detect if a phrase is English
    private val englishWords = hashSetOf(
        "i", "me", "my", "we", "our", "you", "your", "he", "she", "it", "they",
        "this", "that", "the", "a", "an", "is", "am", "are", "was", "were", "be",
        "have", "has", "had", "do", "does", "did", "need", "please", "moment",
        "respond", "yes", "no", "feeling", "overwhelmed", "write", "text",
        "water", "food", "help", "hello", "quiet", "space", "right", "now",
        "can", "could", "would", "what", "where", "when", "why", "how", "fine",
        "thank", "thanks", "sorry", "good", "morning", "night", "want"
    )

    fun resolvePhraseLocale(phraseLang: String, phraseText: String, appLanguage: String): Locale {
        return when (phraseLang) {
            LANG_INDONESIAN, "in" -> Locale.forLanguageTag("id-ID")
            LANG_ENGLISH -> Locale.US
            LANG_AUTO, "" -> detectLocale(phraseText, appLanguage)
            LANG_SYSTEM -> getLocaleForLanguage(appLanguage)
            else -> getLocaleForLanguage(phraseLang)
        }
    }

    /**
     * Automatically detects whether a phrase is Indonesian or English.
     * If uncertain, falls back to the active app language.
     */
    fun detectLocale(phrase: String, appLanguage: String): Locale {
        val tokens = phrase.lowercase(Locale.ROOT)
            .replace(Regex("[^a-zA-Z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        var idCount = 0
        var enCount = 0

        for (token in tokens) {
            if (indonesianWords.contains(token)) idCount++
            if (englishWords.contains(token)) enCount++
        }

        return when {
            idCount > enCount -> Locale.forLanguageTag("id-ID")
            enCount > idCount -> Locale.US
            else -> getLocaleForLanguage(appLanguage)
        }
    }
}
