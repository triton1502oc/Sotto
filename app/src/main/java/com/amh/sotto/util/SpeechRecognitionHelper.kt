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
    private val autoStopDelayMs: Long = 1800L

    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

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

    private fun resetSilenceTimer(onFinalResult: (String) -> Unit) {
        cancelSilenceTimer()
        val runnable = Runnable {
            if (isListening && lastRecognizedText.isNotBlank()) {
                val finalText = lastRecognizedText
                stopListening()
                onFinalResult(finalText)
            }
        }
        silenceRunnable = runnable
        mainHandler.postDelayed(runnable, autoStopDelayMs)
    }

    private fun cleanupInternal() {
        try {
            cancelSilenceTimer()
            lastRecognizedText = ""
            isListening = false
            speechRecognizer?.stopListening()
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
                        android.util.Log.d("SottoSpeech", "onReadyForSpeech (stage=$stage)")
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {
                        android.util.Log.d("SottoSpeech", "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        android.util.Log.d("SottoSpeech", "onEndOfSpeech")
                        isListening = false
                        cancelSilenceTimer()
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        cancelSilenceTimer()
                        android.util.Log.e("SottoSpeech", "SpeechRecognizer onError: $error (stage=$stage)")

                        if (stage == RecognitionStage.ON_DEVICE_OFFLINE && isMissingOfflineModelError(error)) {
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

                        if (stage == RecognitionStage.STANDARD_OFFLINE && isMissingOfflineModelError(error)) {
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

                        onError(error)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        cancelSilenceTimer()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        android.util.Log.d("SottoSpeech", "onResults: $matches")
                        if (!matches.isNullOrEmpty()) {
                            lastRecognizedText = matches[0]
                            onFinalResult(matches[0])
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        android.util.Log.d("SottoSpeech", "onPartialResults: $matches")
                        if (!matches.isNullOrEmpty()) {
                            val partial = matches[0]
                            lastRecognizedText = partial
                            onPartialResult(partial)
                            resetSilenceTimer(onFinalResult)
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
