package com.amh.sotto.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationHelper {

    private val translators = mutableMapOf<Pair<String, String>, Translator>()

    fun toTranslateLanguage(code: String): String? {
        return when (code.lowercase()) {
            "en" -> TranslateLanguage.ENGLISH
            "id", "in" -> TranslateLanguage.INDONESIAN
            else -> TranslateLanguage.fromLanguageTag(code)
        }
    }

    fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
        onProgress: (Boolean) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val sourceLang = toTranslateLanguage(sourceLangCode) ?: TranslateLanguage.ENGLISH
        val targetLang = toTranslateLanguage(targetLangCode) ?: TranslateLanguage.INDONESIAN

        val key = sourceLang to targetLang
        val translator = translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }

        onProgress(true)
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onProgress(false)
                        onSuccess(translatedText)
                    }
                    .addOnFailureListener { e ->
                        onProgress(false)
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                onProgress(false)
                onError(e)
            }
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
