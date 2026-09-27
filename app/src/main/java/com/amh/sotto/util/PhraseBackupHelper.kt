package com.amh.sotto.util

import com.amh.sotto.data.Phrase
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val categories: List<String> = emptyList(),
    val phrases: List<Phrase> = emptyList()
)

data class ImportResult(
    val phraseCount: Int,
    val categoryCount: Int
)

object PhraseBackupHelper {

    const val CURRENT_VERSION = 1

    /**
     * Serializes phrases and category order into a human-readable JSON string.
     */
    fun exportToJson(phrases: List<Phrase>, categories: List<String>): String {
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val categoriesArray = JSONArray()
        val cleanCategories = mutableListOf<String>()
        categories.forEach { cat ->
            val trimmed = cat.trim()
            if (trimmed.isNotBlank() && !trimmed.equals("ALL", ignoreCase = true) && !cleanCategories.any { it.equals(trimmed, ignoreCase = true) }) {
                cleanCategories.add(trimmed)
                categoriesArray.put(trimmed)
            }
        }
        root.put("categories", categoriesArray)

        val phrasesArray = JSONArray()
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
            phrasesArray.put(obj)
        }
        root.put("phrases", phrasesArray)

        return root.toString(2)
    }

    /**
     * Parses a JSON string containing either a structured backup object or a raw array of phrases.
     * Throws IllegalArgumentException if the JSON is malformed or contains no valid phrases.
     */
    fun parseBackupJson(jsonString: String): BackupData {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("Backup content is empty")
        }

        val categoriesList = mutableListOf<String>()
        val phrasesList = mutableListOf<Phrase>()
        var version = CURRENT_VERSION
        var exportedAt = System.currentTimeMillis()

        if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            version = root.optInt("version", CURRENT_VERSION)
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            val categoriesArray = root.optJSONArray("categories")
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    val cat = categoriesArray.optString(i, "").trim()
                    if (cat.isNotBlank() && !cat.equals("ALL", ignoreCase = true) && !categoriesList.any { it.equals(cat, ignoreCase = true) }) {
                        categoriesList.add(cat)
                    }
                }
            }

            val phrasesArray = root.optJSONArray("phrases")
            if (phrasesArray != null) {
                parsePhrasesArray(phrasesArray, phrasesList)
            }
        } else if (trimmed.startsWith("[")) {
            val phrasesArray = JSONArray(trimmed)
            parsePhrasesArray(phrasesArray, phrasesList)
        } else {
            throw IllegalArgumentException("Unrecognized backup format")
        }

        if (phrasesList.isEmpty()) {
            throw IllegalArgumentException("No valid phrases found in backup")
        }

        // Ensure extracted phrases have their categories represented in categoriesList
        phrasesList.forEach { phrase ->
            val cat = phrase.category.trim()
            if (cat.isNotBlank() && !cat.equals("ALL", ignoreCase = true) && !categoriesList.any { it.equals(cat, ignoreCase = true) }) {
                categoriesList.add(cat)
            }
        }

        // Guarantee essential categories exist
        ensureSystemCategories(categoriesList)

        return BackupData(
            version = version,
            exportedAt = exportedAt,
            categories = categoriesList,
            phrases = phrasesList
        )
    }

    private fun parsePhrasesArray(array: JSONArray, targetList: MutableList<Phrase>) {
        for (i in 0 until array.length()) {
            val item = array.get(i)
            if (item is JSONObject) {
                val text = item.optString("text", "").trim()
                if (text.isNotBlank()) {
                    val spokenText = item.optString("spokenText", "").trim().takeIf { it.isNotBlank() && it != LocaleHelper.LANG_AUTO }
                    val spokenLanguage = item.optString("spokenLanguage", "").trim().takeIf { it.isNotBlank() && it != LocaleHelper.LANG_AUTO }
                    val lang = item.optString("language", LocaleHelper.LANG_AUTO).ifBlank { LocaleHelper.LANG_AUTO }
                    val isEmergency = item.optBoolean("isEmergency", false)
                    val rawCategory = item.optString("category", if (isEmergency) Phrase.CATEGORY_EMERGENCY else Phrase.CATEGORY_GENERAL).trim()
                    val category = if (isEmergency) {
                        Phrase.CATEGORY_EMERGENCY
                    } else if (rawCategory.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true) || rawCategory.isBlank()) {
                        Phrase.CATEGORY_GENERAL
                    } else {
                        rawCategory
                    }

                    targetList.add(
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
                targetList.add(
                    Phrase(
                        text = item.trim(),
                        language = LocaleHelper.LANG_AUTO,
                        isEmergency = false,
                        category = Phrase.CATEGORY_GENERAL
                    )
                )
            }
        }
    }

    private fun ensureSystemCategories(list: MutableList<String>) {
        if (!list.any { it.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true) }) {
            list.add(0, Phrase.CATEGORY_EMERGENCY)
        }
        if (!list.any { it.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true) }) {
            list.add(Phrase.CATEGORY_GENERAL)
        }
    }

    /**
     * Merges imported data with current data.
     * Deduplicates phrases by (text + category) and preserves existing ordering.
     */
    fun mergeData(
        currentPhrases: List<Phrase>,
        currentCategories: List<String>,
        backup: BackupData
    ): Pair<List<Phrase>, List<String>> {
        val mergedCategories = currentCategories.toMutableList()
        ensureSystemCategories(mergedCategories)

        backup.categories.forEach { cat ->
            val trimmed = cat.trim()
            if (trimmed.isNotBlank() && !trimmed.equals("ALL", ignoreCase = true) && !mergedCategories.any { it.equals(trimmed, ignoreCase = true) }) {
                val generalIndex = mergedCategories.indexOfFirst { it.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true) }
                if (generalIndex != -1) {
                    mergedCategories.add(generalIndex, trimmed)
                } else {
                    mergedCategories.add(trimmed)
                }
            }
        }

        val mergedPhrases = currentPhrases.toMutableList()
        backup.phrases.forEach { newPhrase ->
            val exists = mergedPhrases.any { existing ->
                existing.text.trim().equals(newPhrase.text.trim(), ignoreCase = true) &&
                    existing.category.equals(newPhrase.category, ignoreCase = true)
            }
            if (!exists) {
                mergedPhrases.add(newPhrase)
                // Ensure the category exists in mergedCategories
                val cat = newPhrase.category.trim()
                if (cat.isNotBlank() && !cat.equals("ALL", ignoreCase = true) && !mergedCategories.any { it.equals(cat, ignoreCase = true) }) {
                    val generalIndex = mergedCategories.indexOfFirst { it.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true) }
                    if (generalIndex != -1) {
                        mergedCategories.add(generalIndex, cat)
                    } else {
                        mergedCategories.add(cat)
                    }
                }
            }
        }

        return Pair(mergedPhrases, mergedCategories)
    }

    /**
     * Replaces current data with backup data.
     * Preserves protected system categories and ensures an emergency phrase is present.
     */
    fun replaceData(
        currentPhrases: List<Phrase>,
        backup: BackupData
    ): Pair<List<Phrase>, List<String>> {
        val newCategories = backup.categories.toMutableList()
        ensureSystemCategories(newCategories)

        val newPhrases = backup.phrases.toMutableList()
        // If backup has no emergency card, retain existing emergency card(s)
        if (newPhrases.none { it.isEmergency }) {
            val existingEmergency = currentPhrases.filter { it.isEmergency }
            if (existingEmergency.isNotEmpty()) {
                newPhrases.addAll(0, existingEmergency)
            }
        }

        // Guarantee all categories referenced by phrases are included
        newPhrases.forEach { phrase ->
            val cat = phrase.category.trim()
            if (cat.isNotBlank() && !cat.equals("ALL", ignoreCase = true) && !newCategories.any { it.equals(cat, ignoreCase = true) }) {
                val generalIndex = newCategories.indexOfFirst { it.equals(Phrase.CATEGORY_GENERAL, ignoreCase = true) }
                if (generalIndex != -1) {
                    newCategories.add(generalIndex, cat)
                } else {
                    newCategories.add(cat)
                }
            }
        }

        return Pair(newPhrases, newCategories)
    }
}
