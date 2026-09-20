package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences
import com.amh.sotto.R
import com.amh.sotto.util.LocaleHelper
import org.json.JSONArray
import org.json.JSONObject

data class Phrase(
    val text: String,
    val language: String = LocaleHelper.LANG_AUTO,
    val spokenText: String? = null,
    val spokenLanguage: String? = null,
    val isEmergency: Boolean = false,
    val category: String = CATEGORY_GENERAL
) {
    companion object {
        const val CATEGORY_GENERAL = "General"
        const val CATEGORY_EMERGENCY = "Emergency"
        const val CATEGORY_NEEDS = "Needs"
        const val CATEGORY_SOCIAL = "Social"
    }
}

interface PhraseRepository {
    fun getPhrases(): List<Phrase>
    fun savePhrases(phrases: List<Phrase>)
}

class SharedPreferencesPhraseRepository(private val context: Context) : PhraseRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("sotto_prefs", Context.MODE_PRIVATE)
    private val KEY_PHRASES = "saved_phrases"
    private val KEY_V2_MIGRATED = "v2_migrated"

    private val fallbackPhrases = listOf(
        // Emergency
        Phrase("I cannot speak right now. Please read my screen.", LocaleHelper.LANG_AUTO, isEmergency = true, category = Phrase.CATEGORY_EMERGENCY),
        // Needs
        Phrase("I need a quiet space.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_NEEDS),
        Phrase("Please give me time.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_NEEDS),
        Phrase("I need to leave now.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_NEEDS),
        // Social
        Phrase("Yes, please.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_SOCIAL),
        Phrase("No, thank you.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_SOCIAL),
        Phrase("Thank you.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_SOCIAL),
        // General
        Phrase("Hello.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_GENERAL),
        Phrase("Please repeat that.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_GENERAL)
    )

    private fun getDefaultPhrases(): List<Phrase> {
        return try {
            val emergency = context.resources.getStringArray(R.array.default_phrases_emergency).map {
                Phrase(text = it, language = LocaleHelper.LANG_AUTO, isEmergency = true, category = Phrase.CATEGORY_EMERGENCY)
            }
            val needs = context.resources.getStringArray(R.array.default_phrases_needs).map {
                Phrase(text = it, language = LocaleHelper.LANG_AUTO, isEmergency = false, category = Phrase.CATEGORY_NEEDS)
            }
            val social = context.resources.getStringArray(R.array.default_phrases_social).map {
                Phrase(text = it, language = LocaleHelper.LANG_AUTO, isEmergency = false, category = Phrase.CATEGORY_SOCIAL)
            }
            val general = context.resources.getStringArray(R.array.default_phrases_general).map {
                Phrase(text = it, language = LocaleHelper.LANG_AUTO, isEmergency = false, category = Phrase.CATEGORY_GENERAL)
            }
            val all = emergency + needs + social + general
            if (all.isNotEmpty()) all else fallbackPhrases
        } catch (e: Exception) {
            fallbackPhrases
        }
    }

    override fun getPhrases(): List<Phrase> {
        val phrasesJson = prefs.getString(KEY_PHRASES, null)
        if (phrasesJson == null) {
            val defaults = getDefaultPhrases()
            savePhrases(defaults)
            prefs.edit().putBoolean(KEY_V2_MIGRATED, true).apply()
            return defaults
        }

        return try {
            val jsonArray = JSONArray(phrasesJson)
            val list = mutableListOf<Phrase>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.get(i)
                if (item is JSONObject) {
                    val text = item.optString("text", "")
                    val spokenText = item.optString("spokenText", "").takeIf { it.isNotBlank() && it != LocaleHelper.LANG_AUTO }
                    val spokenLanguage = item.optString("spokenLanguage", "").takeIf { it.isNotBlank() && it != LocaleHelper.LANG_AUTO }
                    val lang = item.optString("language", LocaleHelper.LANG_AUTO)
                    val isEmergency = item.optBoolean("isEmergency", false)
                    val rawCategory = item.optString("category", if (isEmergency) Phrase.CATEGORY_EMERGENCY else Phrase.CATEGORY_GENERAL)
                    val category = if (isEmergency) Phrase.CATEGORY_EMERGENCY else if (rawCategory == Phrase.CATEGORY_EMERGENCY) Phrase.CATEGORY_GENERAL else rawCategory
                    if (text.isNotBlank()) {
                        list.add(
                            Phrase(
                                text = text,
                                language = lang,
                                spokenText = spokenText,
                                spokenLanguage = spokenLanguage,
                                isEmergency = isEmergency,
                                category = category
                            )
                        )
                    }
                } else if (item is String && item.isNotBlank()) {
                    list.add(Phrase(text = item, language = LocaleHelper.LANG_AUTO, isEmergency = false, category = Phrase.CATEGORY_GENERAL))
                }
            }

            // One-time upgrade: If user upgraded and has no emergency card, prepend the localized default emergency card
            val isMigrated = prefs.getBoolean(KEY_V2_MIGRATED, false)
            if (!isMigrated) {
                if (list.none { it.isEmergency }) {
                    val emergencyDefault = getDefaultPhrases().firstOrNull { it.isEmergency }
                    if (emergencyDefault != null) {
                        list.add(0, emergencyDefault)
                    }
                }
                prefs.edit().putBoolean(KEY_V2_MIGRATED, true).apply()
                savePhrases(list)
            }

            if (list.isNotEmpty()) list else getDefaultPhrases()
        } catch (e: Exception) {
            getDefaultPhrases()
        }
    }

    override fun savePhrases(phrases: List<Phrase>) {
        val jsonArray = JSONArray()
        phrases.forEach { phrase ->
            val obj = JSONObject()
            obj.put("text", phrase.text)
            if (!phrase.spokenText.isNullOrBlank()) {
                obj.put("spokenText", phrase.spokenText)
            }
            if (!phrase.spokenLanguage.isNullOrBlank()) {
                obj.put("spokenLanguage", phrase.spokenLanguage)
            }
            obj.put("language", phrase.language)
            obj.put("isEmergency", phrase.isEmergency)
            obj.put("category", phrase.category)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PHRASES, jsonArray.toString()).apply()
    }
}
