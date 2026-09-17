package com.example.sotto.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

interface PhraseRepository {
    fun getPhrases(): List<String>
    fun savePhrases(phrases: List<String>)
}

class SharedPreferencesPhraseRepository(context: Context) : PhraseRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("sotto_prefs", Context.MODE_PRIVATE)
    private val KEY_PHRASES = "saved_phrases"

    private val defaultPhrases = listOf(
        "I need a quiet space, please.",
        "Please give me a moment to respond.",
        "Yes, that is fine.",
        "No, not right now.",
        "I am feeling overwhelmed.",
        "Can you write that down or text me?"
    )

    override fun getPhrases(): List<String> {
        val phrasesJson = prefs.getString(KEY_PHRASES, null)
        if (phrasesJson == null) {
            savePhrases(defaultPhrases)
            return defaultPhrases
        }
        
        return try {
            val jsonArray = JSONArray(phrasesJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            defaultPhrases
        }
    }

    override fun savePhrases(phrases: List<String>) {
        val jsonArray = JSONArray()
        phrases.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_PHRASES, jsonArray.toString()).apply()
    }
}
