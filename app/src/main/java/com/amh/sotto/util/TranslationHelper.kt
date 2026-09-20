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

object TranslationHelper {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val translators = mutableMapOf<Pair<String, String>, Translator>()
    private val modelManager = RemoteModelManager.getInstance()

    fun toTranslateLanguage(code: String): String? {
        return when (code.lowercase()) {
            "en" -> TranslateLanguage.ENGLISH
            "id", "in" -> TranslateLanguage.INDONESIAN
            else -> TranslateLanguage.fromLanguageTag(code)
        }
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
                    mainHandler.post {
                        onProgress(false)
                        onSuccess()
                    }
                }
                .addOnFailureListener { e ->
                    mainHandler.post {
                        onProgress(false)
                        onError(e)
                    }
                }
        } catch (t: Throwable) {
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
                            mainHandler.post {
                                onProgress(false)
                                onError(e)
                            }
                        }
                }
                .addOnFailureListener { e ->
                    mainHandler.post {
                        onProgress(false)
                        onError(e)
                    }
                }
        } catch (t: Throwable) {
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
