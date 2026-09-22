package com.amh.sotto.util

import android.os.Handler
import android.os.Looper
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

import java.util.Locale

object TranslationHelper {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val translators = mutableMapOf<Pair<String, String>, Translator>()
    private val modelManager = RemoteModelManager.getInstance()

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

    fun toTranslateLanguage(code: String): String? {
        val normalized = code.lowercase(Locale.ROOT).trim()
        when (normalized) {
            "en", "eng" -> return TranslateLanguage.ENGLISH
            "id", "in", "ind" -> return TranslateLanguage.INDONESIAN
        }
        // Direct match (e.g. "de", "it", "es")
        val direct = TranslateLanguage.fromLanguageTag(normalized)
        if (direct != null) return direct

        // 3-letter ISO-639-2 match from TTS engines (e.g. "deu" -> "de", "ita" -> "it")
        val fromIso3 = iso3ToIso1Map[normalized]?.let { TranslateLanguage.fromLanguageTag(it) }
        if (fromIso3 != null) return fromIso3

        // Prefix match for regional tags (e.g. "de-DE", "de_DE")
        val basePart = normalized.substringBefore('-').substringBefore('_')
        val fromBase = TranslateLanguage.fromLanguageTag(basePart)
            ?: iso3ToIso1Map[basePart]?.let { TranslateLanguage.fromLanguageTag(it) }
        return fromBase
    }

    /**
     * Checks whether the on-device language model for the given language code is downloaded.
     */
    fun isModelDownloaded(langCode: String, onResult: (Boolean) -> Unit) {
        val translateLang = toTranslateLanguage(langCode)
        if (translateLang == null) {
            mainHandler.post { onResult(false) }
            return
        }
        try {
            val model = TranslateRemoteModel.Builder(translateLang).build()
            modelManager.isModelDownloaded(model)
                .addOnSuccessListener { downloaded ->
                    mainHandler.post { onResult(downloaded) }
                }
                .addOnFailureListener {
                    mainHandler.post { onResult(false) }
                }
        } catch (t: Throwable) {
            mainHandler.post { onResult(false) }
        }
    }

    /**
     * Downloads the on-device language model for the given language code.
     */
    fun downloadModel(
        langCode: String,
        onProgress: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val translateLang = toTranslateLanguage(langCode)
        if (translateLang == null) {
            mainHandler.post { onError(IllegalArgumentException("Unsupported language: $langCode")) }
            return
        }
        try {
            val model = TranslateRemoteModel.Builder(translateLang).build()
            val conditions = DownloadConditions.Builder().build()
            mainHandler.post { onProgress(true) }
            modelManager.download(model, conditions)
                .addOnSuccessListener {
                    android.util.Log.d("TranslationHelper", "Download SUCCESS for $langCode")
                    mainHandler.post {
                        onProgress(false)
                        onSuccess()
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("TranslationHelper", "Download FAILED for $langCode", e)
                    mainHandler.post {
                        onProgress(false)
                        onError(e)
                    }
                }
        } catch (t: Throwable) {
            android.util.Log.e("TranslationHelper", "Download EXCEPTION for $langCode", t)
            mainHandler.post {
                onProgress(false)
                onError(if (t is Exception) t else Exception(t))
            }
        }
    }

    /**
     * Translates text from sourceLangCode to targetLangCode.
     * Guarantees zero crashes: all operations are guarded by try-catch and callbacks are dispatched on the main thread.
     */
    fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
        onProgress: (Boolean) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
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

            mainHandler.post { onProgress(true) }
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            mainHandler.post {
                                onProgress(false)
                                onSuccess(translatedText)
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("TranslationHelper", "Translation execution FAILED", e)
                            mainHandler.post {
                                onProgress(false)
                                onError(e)
                            }
                        }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("TranslationHelper", "downloadModelIfNeeded FAILED", e)
                    mainHandler.post {
                        onProgress(false)
                        onError(e)
                    }
                }
        } catch (t: Throwable) {
            android.util.Log.e("TranslationHelper", "translate EXCEPTION", t)
            mainHandler.post {
                onProgress(false)
                onError(if (t is Exception) t else Exception(t))
            }
        }
    }

    fun close() {
        try {
            translators.values.forEach { it.close() }
            translators.clear()
        } catch (t: Throwable) {
            // Ignore on cleanup
        }
    }
}
