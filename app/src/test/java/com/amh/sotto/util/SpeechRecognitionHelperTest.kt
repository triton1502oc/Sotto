package com.amh.sotto.util

import android.speech.SpeechRecognizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecognitionHelperTest {

    @Test
    fun `isMissingOfflineModelError detects missing language pack errors`() {
        assertTrue(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognitionHelper.ERROR_LANGUAGE_UNAVAILABLE))
        assertTrue(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognitionHelper.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertTrue(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognitionHelper.ERROR_SERVER_DISCONNECTED))
    }

    @Test
    fun `isMissingOfflineModelError returns false for regular timeout or no match`() {
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_NO_MATCH))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_AUDIO))
    }
}
