package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences
import com.amh.sotto.R
import com.amh.sotto.util.LocaleHelper
import org.json.JSONArray
import org.json.JSONObject

data class Phrase(
    val text: String,
    val language: String = LocaleHelper.LANG_AUTO
)

interface PhraseRepository {
    fun getPhrases(): List<Phrase>
    fun savePhrases(phrases: List<Phrase>)
}

class SharedPreferencesPhraseRepository(private val context: Context) : PhraseRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("sotto_prefs", Context.MODE_PRIVATE)
    private val KEY_PHRASES = "saved_phrases"

    private val fallbackPhrases = listOf(
        Phrase("I need a quiet space, please.", LocaleHelper.LANG_AUTO),
        Phrase("Please give me a moment to respond.", LocaleHelper.LANG_AUTO),
        Phrase("Yes, that is fine.", LocaleHelper.LANG_AUTO),
        Phrase("No, not right now.", LocaleHelper.LANG_AUTO),
        Phrase("I am feeling overwhelmed.", LocaleHelper.LANG_AUTO),
        Phrase("Can you write that down or text me?", LocaleHelper.LANG_AUTO)
    )

    private fun getDefaultPhrases(): List<Phrase> {
        return try {
            val array = context.resources.getStringArray(R.array.default_phrases)
            if (array.isNotEmpty()) {
                array.map { Phrase(text = it, language = LocaleHelper.LANG_AUTO) }
            } else {
                fallbackPhrases
            }
        } catch (e: Exception) {
            fallbackPhrases
        }
    }

    override fun getPhrases(): List<Phrase> {
        val phrasesJson = prefs.getString(KEY_PHRASES, null)
        if (phrasesJson == null) {
            val defaults = getDefaultPhrases()
            savePhrases(defaults)
            return defaults
        }

        return try {
            val jsonArray = JSONArray(phrasesJson)
            val list = mutableListOf<Phrase>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.get(i)
                if (item is JSONObject) {
                    val text = item.optString("text", "")
                    val lang = item.optString("language", LocaleHelper.LANG_AUTO)
                    if (text.isNotBlank()) {
                        list.add(Phrase(text = text, language = lang))
                    }
                } else if (item is String && item.isNotBlank()) {
                    list.add(Phrase(text = item, language = LocaleHelper.LANG_AUTO))
                }
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
            obj.put("language", phrase.language)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PHRASES, jsonArray.toString()).apply()
    }
}
