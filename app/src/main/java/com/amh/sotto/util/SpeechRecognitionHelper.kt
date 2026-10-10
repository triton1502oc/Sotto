package com.amh.sotto.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    fun startListening(
        locale: Locale,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        runOnMainThread {
            try {
                cleanupInternal()

                val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
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
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, autoStopDelayMs)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, autoStopDelayMs)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        android.util.Log.d("SottoSpeech", "onReadyForSpeech")
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
                        android.util.Log.e("SottoSpeech", "SpeechRecognizer onError: $error")
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
                android.util.Log.e("SottoSpeech", "SpeechRecognizer exception on startListening", e)
                isListening = false
                cancelSilenceTimer()
                onError(SpeechRecognizer.ERROR_CLIENT)
            }
        }
    }

    fun stopListening() {
        runOnMainThread {
            cleanupInternal()
        }
    }

    fun destroy() {
        runOnMainThread {
            cleanupInternal()
        }
    }
}
