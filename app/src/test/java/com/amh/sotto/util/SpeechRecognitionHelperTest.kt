package com.amh.sotto.util

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `isMissingOfflineModelError returns false for regular timeout or no match or client error`() {
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_NO_MATCH))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_AUDIO))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_CLIENT))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_NETWORK))
        assertFalse(SpeechRecognitionHelper.isMissingOfflineModelError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
    }

    @Test
    fun `canFallbackStage returns true for missing model errors and client error`() {
        assertTrue(SpeechRecognitionHelper.canFallbackStage(SpeechRecognitionHelper.ERROR_LANGUAGE_UNAVAILABLE))
        assertTrue(SpeechRecognitionHelper.canFallbackStage(SpeechRecognitionHelper.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertTrue(SpeechRecognitionHelper.canFallbackStage(SpeechRecognitionHelper.ERROR_SERVER_DISCONNECTED))
        assertTrue(SpeechRecognitionHelper.canFallbackStage(SpeechRecognizer.ERROR_CLIENT))
    }

    @Test
    fun `canFallbackStage returns false for non-fallback errors like timeout or no match`() {
        assertFalse(SpeechRecognitionHelper.canFallbackStage(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertFalse(SpeechRecognitionHelper.canFallbackStage(SpeechRecognizer.ERROR_NO_MATCH))
        assertFalse(SpeechRecognitionHelper.canFallbackStage(SpeechRecognizer.ERROR_AUDIO))
        assertFalse(SpeechRecognitionHelper.canFallbackStage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertFalse(SpeechRecognitionHelper.canFallbackStage(SpeechRecognizer.ERROR_NETWORK))
    }

    @Test
    fun `resolveRecognitionResult selects first non-blank match`() {
        val matches = listOf("Hello world", "Hello there")
        assertEquals("Hello world", SpeechRecognitionHelper.resolveRecognitionResult(matches, "fallback"))
    }

    @Test
    fun `resolveRecognitionResult skips blank matches and picks first valid text`() {
        val matches = listOf("", "   ", "Valid speech result")
        assertEquals("Valid speech result", SpeechRecognitionHelper.resolveRecognitionResult(matches, "fallback"))
    }

    @Test
    fun `resolveRecognitionResult uses fallback text when matches are null or empty`() {
        assertEquals("partial speech", SpeechRecognitionHelper.resolveRecognitionResult(null, "partial speech"))
        assertEquals("partial speech", SpeechRecognitionHelper.resolveRecognitionResult(emptyList(), "partial speech"))
        assertEquals("partial speech", SpeechRecognitionHelper.resolveRecognitionResult(listOf("", "  "), "partial speech"))
    }

    @Test
    fun `resolveRecognitionResult returns null when matches and fallback are both empty`() {
        assertNull(SpeechRecognitionHelper.resolveRecognitionResult(null, ""))
        assertNull(SpeechRecognitionHelper.resolveRecognitionResult(emptyList(), "   "))
        assertNull(SpeechRecognitionHelper.resolveRecognitionResult(listOf("", "  "), ""))
    }

    @Test
    fun `normalizeHypothesis collapses progressive repetitions while preserving valid reduplications`() {
        assertEquals("I need water", SpeechRecognitionHelper.normalizeHypothesis("I I need I need water"))
        assertEquals("I need help", SpeechRecognitionHelper.normalizeHypothesis("I need I need help"))
        assertEquals("no no", SpeechRecognitionHelper.normalizeHypothesis("no no"))
        assertEquals("sama sama", SpeechRecognitionHelper.normalizeHypothesis("sama sama"))
        assertEquals("", SpeechRecognitionHelper.normalizeHypothesis("   "))
    }

    @Test
    fun `mergeDictationText combines base text and hypothesis without duplicate overlapping text`() {
        assertEquals("Hello world", SpeechRecognitionHelper.mergeDictationText("", "Hello world"))
        assertEquals("Hello world", SpeechRecognitionHelper.mergeDictationText("Hello world", ""))
        assertEquals("Hello world", SpeechRecognitionHelper.mergeDictationText("Hello world", "Hello world"))
        assertEquals("Hello world", SpeechRecognitionHelper.mergeDictationText("Hello", "Hello world"))
        assertEquals("Hello world", SpeechRecognitionHelper.mergeDictationText("Hello world", "world"))
        assertEquals(
            "I want to go to the park",
            SpeechRecognitionHelper.mergeDictationText("I want to go to", "go to the park")
        )
        assertEquals("Hello world", SpeechRecognitionHelper.mergeDictationText("Hello", "world"))
    }
}
