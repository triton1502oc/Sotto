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
        const val CATEGORY_SAFETY = CATEGORY_EMERGENCY
        const val CATEGORY_NEEDS = "Needs"
        const val CATEGORY_SOCIAL = "Social"
        const val CATEGORY_CARE = "Care"

        val DEFAULT_CATEGORIES = listOf(
            CATEGORY_EMERGENCY,
            CATEGORY_NEEDS,
            CATEGORY_SOCIAL,
            CATEGORY_CARE,
            CATEGORY_GENERAL
        )

        // Retained for backwards compatibility
        val SYSTEM_CATEGORIES = DEFAULT_CATEGORIES

        fun isSystemCategory(category: String): Boolean {
            return category.equals("ALL", ignoreCase = true) ||
                category.equals(CATEGORY_EMERGENCY, ignoreCase = true) ||
                category.equals(CATEGORY_SAFETY, ignoreCase = true) ||
                category.equals(CATEGORY_GENERAL, ignoreCase = true)
        }

        fun isUndeletableCategory(category: String): Boolean {
            return category.equals("ALL", ignoreCase = true) ||
                category.equals(CATEGORY_EMERGENCY, ignoreCase = true) ||
                category.equals(CATEGORY_SAFETY, ignoreCase = true) ||
                category.equals(CATEGORY_GENERAL, ignoreCase = true)
        }

        fun isUnrenamableCategory(category: String): Boolean {
            return category.equals("ALL", ignoreCase = true) ||
                category.equals(CATEGORY_EMERGENCY, ignoreCase = true) ||
                category.equals(CATEGORY_SAFETY, ignoreCase = true)
        }
    }
}

interface PhraseRepository {
    fun getPhrases(): List<Phrase>
    fun savePhrases(phrases: List<Phrase>)
    fun getCategories(): List<String>
    fun saveCategories(categories: List<String>)
    fun getCustomCategories(): List<String>
    fun saveCustomCategories(categories: List<String>)
}

class SharedPreferencesPhraseRepository(private val context: Context) : PhraseRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("sotto_prefs", Context.MODE_PRIVATE)
    private val KEY_PHRASES = "saved_phrases"
    private val KEY_CUSTOM_CATEGORIES = "saved_custom_categories"
    private val KEY_CATEGORY_ORDER = "saved_category_order"
    private val KEY_V2_MIGRATED = "v2_migrated"
    private val KEY_CARE_MIGRATED = "care_migrated"
    private val KEY_SAFETY_TEMPLATES_MIGRATED = "safety_templates_migrated"

    private val fallbackPhrases = listOf(
        // Safety
        Phrase("I cannot speak right now. Please read my screen.", LocaleHelper.LANG_AUTO, isEmergency = true, category = Phrase.CATEGORY_EMERGENCY),
        Phrase("Please call my emergency contact: [Phone Number]", LocaleHelper.LANG_AUTO, isEmergency = true, category = Phrase.CATEGORY_EMERGENCY),
        Phrase("I live around [Area / Neighborhood]. Please call my contact.", LocaleHelper.LANG_AUTO, isEmergency = true, category = Phrase.CATEGORY_EMERGENCY),
        // Needs
        Phrase("I need a quiet space.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_NEEDS),
        Phrase("Please give me time.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_NEEDS),
        Phrase("I need to leave now.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_NEEDS),
        // Social
        Phrase("Yes, please.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_SOCIAL),
        Phrase("No, thank you.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_SOCIAL),
        Phrase("Thank you.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_SOCIAL),
        // Care
        Phrase("Are you in pain?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
        Phrase("Do you need water or food?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
        Phrase("Do you need the restroom?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
        Phrase("Are you feeling cold or warm?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
        Phrase("Do you want to rest or sleep?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
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
            val care = try {
                context.resources.getStringArray(R.array.default_phrases_care).map {
                    Phrase(text = it, language = LocaleHelper.LANG_AUTO, isEmergency = false, category = Phrase.CATEGORY_CARE)
                }
            } catch (e: Exception) {
                emptyList()
            }
            val general = context.resources.getStringArray(R.array.default_phrases_general).map {
                Phrase(text = it, language = LocaleHelper.LANG_AUTO, isEmergency = false, category = Phrase.CATEGORY_GENERAL)
            }
            val all = emergency + needs + social + care + general
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

            // One-time upgrade: Add Care category default cards if not present
            val isCareMigrated = prefs.getBoolean(KEY_CARE_MIGRATED, false)
            if (!isCareMigrated) {
                if (list.none { it.category == Phrase.CATEGORY_CARE }) {
                    val defaultCare = getDefaultPhrases().filter { it.category == Phrase.CATEGORY_CARE }
                    list.addAll(defaultCare)
                }
                prefs.edit().putBoolean(KEY_CARE_MIGRATED, true).apply()
                savePhrases(list)
            }

            // One-time upgrade: Add new Safety contact/area templates if missing
            val isSafetyMigrated = prefs.getBoolean(KEY_SAFETY_TEMPLATES_MIGRATED, false)
            if (!isSafetyMigrated) {
                val emergencyDefaults = getDefaultPhrases().filter { it.isEmergency }
                val existingTexts = list.map { it.text }.toSet()
                val missingDefaults = emergencyDefaults.filter { it.text !in existingTexts }
                if (missingDefaults.isNotEmpty()) {
                    val lastEmergencyIndex = list.indexOfLast { it.isEmergency }
                    if (lastEmergencyIndex != -1) {
                        list.addAll(lastEmergencyIndex + 1, missingDefaults)
                    } else {
                        list.addAll(0, missingDefaults)
                    }
                }
                prefs.edit().putBoolean(KEY_SAFETY_TEMPLATES_MIGRATED, true).apply()
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

    override fun getCategories(): List<String> {
        val json = prefs.getString(KEY_CATEGORY_ORDER, null)
        if (json == null) {
            val legacyCustom = getCustomCategories()
            val list = Phrase.DEFAULT_CATEGORIES.toMutableList()
            legacyCustom.forEach { custom ->
                if (!list.any { it.equals(custom, ignoreCase = true) }) {
                    list.add(custom)
                }
            }
            saveCategories(list)
            return list
        }

        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val cat = jsonArray.optString(i, "").trim()
                if (cat.isNotBlank() && !cat.equals("ALL", ignoreCase = true) && !list.any { it.equals(cat, ignoreCase = true) }) {
                    list.add(cat)
                }
            }
            if (!list.any { it.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true) }) {
                list.add(0, Phrase.CATEGORY_EMERGENCY)
            }
            if (!list.any { it.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true) }) {
                list.add(Phrase.CATEGORY_GENERAL)
            }
            list
        } catch (e: Exception) {
            Phrase.DEFAULT_CATEGORIES
        }
    }

    override fun saveCategories(categories: List<String>) {
        val jsonArray = JSONArray()
        val cleanList = mutableListOf<String>()
        categories.forEach { cat ->
            val trimmed = cat.trim()
            if (trimmed.isNotBlank() && !trimmed.equals("ALL", ignoreCase = true) && !cleanList.any { it.equals(trimmed, ignoreCase = true) }) {
                cleanList.add(trimmed)
                jsonArray.put(trimmed)
            }
        }
        if (!cleanList.any { it.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true) }) {
            cleanList.add(0, Phrase.CATEGORY_EMERGENCY)
            jsonArray.put(Phrase.CATEGORY_EMERGENCY)
        }
        if (!cleanList.any { it.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true) }) {
            cleanList.add(Phrase.CATEGORY_GENERAL)
            jsonArray.put(Phrase.CATEGORY_GENERAL)
        }
        prefs.edit().putString(KEY_CATEGORY_ORDER, jsonArray.toString()).apply()
    }

    override fun getCustomCategories(): List<String> {
        val json = prefs.getString(KEY_CUSTOM_CATEGORIES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val cat = jsonArray.optString(i, "").trim()
                if (cat.isNotBlank() && !Phrase.isUndeletableCategory(cat) && !list.any { it.equals(cat, ignoreCase = true) }) {
                    list.add(cat)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun saveCustomCategories(categories: List<String>) {
        val jsonArray = JSONArray()
        categories.forEach { cat ->
            val trimmed = cat.trim()
            if (trimmed.isNotBlank() && !Phrase.isUndeletableCategory(trimmed)) {
                jsonArray.put(trimmed)
            }
        }
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, jsonArray.toString()).apply()
    }
}
