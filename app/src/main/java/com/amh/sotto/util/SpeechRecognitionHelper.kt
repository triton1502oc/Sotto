package com.amh.sotto.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognitionHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var silenceRunnable: Runnable? = null
    private var lastRecognizedText: String = ""
    private var activeSessionId: Int = 0
    private val autoStopDelayMs: Long = 1800L

    var isListening: Boolean = false
        private set

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun cancelSilenceTimer() {
        silenceRunnable?.let { mainHandler.removeCallbacks(it) }
        silenceRunnable = null
    }

    private fun resetSilenceTimer(sessionId: Int, onFinalResult: (String) -> Unit) {
        cancelSilenceTimer()
        val runnable = Runnable {
            if (sessionId == activeSessionId && isListening && lastRecognizedText.isNotBlank()) {
                val finalText = normalizeHypothesis(lastRecognizedText)
                lastRecognizedText = ""
                cleanupInternal()
                onFinalResult(finalText)
            }
        }
        silenceRunnable = runnable
        mainHandler.postDelayed(runnable, autoStopDelayMs)
    }

    private fun cleanupInternal() {
        try {
            activeSessionId++
            cancelSilenceTimer()
            lastRecognizedText = ""
            isListening = false
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            android.util.Log.e("SottoSpeech", "Error during cleanup", e)
        }
    }

    private enum class RecognitionStage {
        ON_DEVICE_OFFLINE,
        STANDARD_OFFLINE,
        STANDARD_ONLINE
    }

    fun startListening(
        locale: Locale,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        lastRecognizedText = ""
        startListeningInternal(
            locale = locale,
            stage = RecognitionStage.ON_DEVICE_OFFLINE,
            onPartialResult = onPartialResult,
            onFinalResult = onFinalResult,
            onError = onError
        )
    }

    private fun startListeningInternal(
        locale: Locale,
        stage: RecognitionStage,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        runOnMainThread {
            try {
                cleanupInternal()
                val sessionId = activeSessionId

                val recognizer = if (stage == RecognitionStage.ON_DEVICE_OFFLINE &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
                speechRecognizer = recognizer

                val langTag = locale.toLanguageTag()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    if (stage != RecognitionStage.STANDARD_ONLINE) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, autoStopDelayMs)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, autoStopDelayMs)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (sessionId != activeSessionId) return
                        android.util.Log.d("SottoSpeech", "onReadyForSpeech (stage=$stage)")
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {
                        if (sessionId != activeSessionId) return
                        android.util.Log.d("SottoSpeech", "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        if (sessionId != activeSessionId) return
                        android.util.Log.d("SottoSpeech", "onEndOfSpeech")
                        isListening = false
                        cancelSilenceTimer()
                    }

                    override fun onError(error: Int) {
                        if (sessionId != activeSessionId) return
                        isListening = false
                        cancelSilenceTimer()
                        android.util.Log.e("SottoSpeech", "SpeechRecognizer onError: $error (stage=$stage)")

                        if (lastRecognizedText.isNotBlank()) {
                            val finalText = normalizeHypothesis(lastRecognizedText)
                            lastRecognizedText = ""
                            activeSessionId++
                            onFinalResult(finalText)
                            return
                        }

                        val canFallback = canFallbackStage(error)

                        if (stage == RecognitionStage.ON_DEVICE_OFFLINE && canFallback) {
                            android.util.Log.i("SottoSpeech", "Falling back from ON_DEVICE_OFFLINE to STANDARD_OFFLINE for $locale")
                            startListeningInternal(
                                locale = locale,
                                stage = RecognitionStage.STANDARD_OFFLINE,
                                onPartialResult = onPartialResult,
                                onFinalResult = onFinalResult,
                                onError = onError
                            )
                            return
                        }

                        if (stage == RecognitionStage.STANDARD_OFFLINE && canFallback) {
                            android.util.Log.i("SottoSpeech", "Falling back from STANDARD_OFFLINE to STANDARD_ONLINE for $locale")
                            startListeningInternal(
                                locale = locale,
                                stage = RecognitionStage.STANDARD_ONLINE,
                                onPartialResult = onPartialResult,
                                onFinalResult = onFinalResult,
                                onError = onError
                            )
                            return
                        }

                        activeSessionId++
                        onError(error)
                    }

                    override fun onResults(results: Bundle?) {
                        if (sessionId != activeSessionId) return
                        isListening = false
                        cancelSilenceTimer()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        android.util.Log.d("SottoSpeech", "onResults: $matches")
                        val recognized = resolveRecognitionResult(matches, lastRecognizedText)
                        lastRecognizedText = ""
                        activeSessionId++
                        if (recognized != null) {
                            onFinalResult(recognized)
                        } else {
                            onError(SpeechRecognizer.ERROR_NO_MATCH)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (sessionId != activeSessionId) return
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        android.util.Log.d("SottoSpeech", "onPartialResults: $matches")
                        if (!matches.isNullOrEmpty()) {
                            val partial = normalizeHypothesis(matches[0])
                            if (partial.isNotBlank()) {
                                lastRecognizedText = partial
                                onPartialResult(partial)
                                resetSilenceTimer(sessionId, onFinalResult)
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                android.util.Log.e("SottoSpeech", "SpeechRecognizer exception on startListening (stage=$stage)", e)
                isListening = false
                cancelSilenceTimer()
                if (lastRecognizedText.isNotBlank()) {
                    val finalText = normalizeHypothesis(lastRecognizedText)
                    lastRecognizedText = ""
                    activeSessionId++
                    onFinalResult(finalText)
                    return@runOnMainThread
                }
                when (stage) {
                    RecognitionStage.ON_DEVICE_OFFLINE -> {
                        startListeningInternal(
                            locale = locale,
                            stage = RecognitionStage.STANDARD_OFFLINE,
                            onPartialResult = onPartialResult,
                            onFinalResult = onFinalResult,
                            onError = onError
                        )
                    }
                    RecognitionStage.STANDARD_OFFLINE -> {
                        startListeningInternal(
                            locale = locale,
                            stage = RecognitionStage.STANDARD_ONLINE,
                            onPartialResult = onPartialResult,
                            onFinalResult = onFinalResult,
                            onError = onError
                        )
                    }
                    RecognitionStage.STANDARD_ONLINE -> {
                        onError(SpeechRecognizer.ERROR_CLIENT)
                    }
                }
            }
        }
    }

    fun stopListening() {
        runOnMainThread {
            cleanupInternal()
        }
    }

    fun triggerModelDownload(locale: Locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val downloadIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                }
                speechRecognizer?.triggerModelDownload(downloadIntent)
            } catch (e: Exception) {
                android.util.Log.e("SottoSpeech", "Failed to trigger model download", e)
            }
        }
    }

    fun destroy() {
        runOnMainThread {
            cleanupInternal()
        }
    }

    companion object {
        const val ERROR_SERVER_DISCONNECTED = 11
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13

        fun isMissingOfflineModelError(errorCode: Int): Boolean {
            return errorCode == ERROR_LANGUAGE_UNAVAILABLE ||
                errorCode == ERROR_LANGUAGE_NOT_SUPPORTED ||
                errorCode == ERROR_SERVER_DISCONNECTED
        }

        fun canFallbackStage(errorCode: Int): Boolean {
            return isMissingOfflineModelError(errorCode) || errorCode == SpeechRecognizer.ERROR_CLIENT
        }

        fun resolveRecognitionResult(matches: List<String>?, fallbackText: String = ""): String? {
            val raw = matches?.firstOrNull { it.isNotBlank() }
                ?: fallbackText.takeIf { it.isNotBlank() }
            return raw?.let { normalizeHypothesis(it) }?.takeIf { it.isNotBlank() }
        }

        /**
         * Collapses progressive prefix compounding emitted by some on-device/offline STT engines
         * (e.g., "I I need I need water" -> "I need water") while preserving normal single-word
         * reduplication (e.g., "no no", "sama sama").
         */
        fun normalizeHypothesis(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            var words = trimmed.split(Regex("\\s+"))
            var changed = true
            while (changed && words.size >= 2) {
                changed = false
                val maxPrefix = words.size / 2
                for (k in maxPrefix downTo 1) {
                    val prefixMatches = (0 until k).all { idx ->
                        words[idx].equals(words[k + idx], ignoreCase = true)
                    }
                    if (!prefixMatches) continue
                    if (k >= 2) {
                        words = words.drop(k)
                        changed = true
                        break
                    } else {
                        // For single-word prefix repeat (k == 1), only collapse if it's part of a
                        // progressive multi-word buildup (e.g., "I" -> "I need" -> "I need water")
                        val remaining = words.drop(1)
                        val hasProgressiveFollowup = (2..(remaining.size / 2)).any { m ->
                            (0 until m).all { idx ->
                                remaining[idx].equals(remaining[m + idx], ignoreCase = true)
                            }
                        }
                        if (hasProgressiveFollowup) {
                            words = remaining
                            changed = true
                            break
                        }
                    }
                }
            }
            return words.joinToString(" ")
        }

        /**
         * Combines pre-existing draft text (`baseText`) with the current utterance (`hypothesis`)
         * without duplicating overlapping prefixes/suffixes.
         */
        fun mergeDictationText(baseText: String, hypothesis: String): String {
            val base = baseText.trim()
            val hyp = normalizeHypothesis(hypothesis)
            if (base.isEmpty()) return hyp
            if (hyp.isEmpty()) return base
            if (hyp.equals(base, ignoreCase = true)) return hyp
            if (hyp.startsWith(base, ignoreCase = true) &&
                (hyp.length == base.length || hyp[base.length].isWhitespace())
            ) {
                return hyp
            }
            if (base.endsWith(hyp, ignoreCase = true) &&
                (base.length == hyp.length || base[base.length - hyp.length - 1].isWhitespace())
            ) {
                return base
            }
            val baseWords = base.split(Regex("\\s+"))
            val hypWords = hyp.split(Regex("\\s+"))
            val maxOverlap = minOf(baseWords.size, hypWords.size)
            for (k in maxOverlap downTo 1) {
                if (k == 1 && baseWords.size > 1) continue
                val overlaps = (0 until k).all { idx ->
                    baseWords[baseWords.size - k + idx].equals(hypWords[idx], ignoreCase = true)
                }
                if (overlaps) {
                    val mergedWords = baseWords + hypWords.drop(k)
                    return mergedWords.joinToString(" ")
                }
            }
            return "$base $hyp"
        }

        fun createSpeechIntent(
            context: Context,
            prompt: String? = null,
            language: String? = null,
            preferOffline: Boolean = false
        ): Intent {
            val effectiveLang = if (!language.isNullOrBlank() && language != LocaleHelper.LANG_AUTO) {
                language
            } else {
                LocaleHelper.getEffectiveLanguage(context)
            }
            val langTag = LocaleHelper.getLocaleForLanguage(effectiveLang).toLanguageTag()
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                if (!prompt.isNullOrBlank()) {
                    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                }
                if (preferOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }
        }

        fun openOfflineSpeechSettings(context: Context) {
            val intent = Intent("com.google.android.speech.embedded.MANAGE_LANGUAGES").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
