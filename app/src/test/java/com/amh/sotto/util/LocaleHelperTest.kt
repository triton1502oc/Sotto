package com.amh.sotto.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleHelperTest {

    @Test
    fun `detectLocale detects Indonesian phrases`() {
        val phrases = listOf(
            "Saya butuh tempat yang tenang.",
            "Tolong beri saya waktu sebentar untuk merespons.",
            "Ya, tidak apa-apa.",
            "Tidak untuk sekarang.",
            "Saya merasa kewalahan.",
            "Bisa tolong tuliskan atau kirim pesan?",
            "Mau makan nasi goreng",
            "Permisi, tolong bantu saya",
            "Selamat pagi semuanya"
        )

        for (phrase in phrases) {
            val locale = LocaleHelper.detectLocale(phrase, LocaleHelper.LANG_ENGLISH)
            assertEquals("Expected Indonesian for '$phrase'", "id", locale.language)
        }
    }

    @Test
    fun `detectLocale detects English phrases`() {
        val phrases = listOf(
            "I need a quiet space, please.",
            "Please give me a moment to respond.",
            "Yes, that is fine.",
            "No, not right now.",
            "I am feeling overwhelmed.",
            "Can you write that down or text me?",
            "Hello, how are you today?",
            "I need some water please"
        )

        for (phrase in phrases) {
            val locale = LocaleHelper.detectLocale(phrase, LocaleHelper.LANG_INDONESIAN)
            assertEquals("Expected English for '$phrase'", "en", locale.language)
        }
    }

    @Test
    fun `detectLocale falls back to active app language when ambiguous`() {
        val ambiguousPhrase = "123 456"
        
        val localeEn = LocaleHelper.detectLocale(ambiguousPhrase, LocaleHelper.LANG_ENGLISH)
        assertEquals("en", localeEn.language)

        val localeId = LocaleHelper.detectLocale(ambiguousPhrase, LocaleHelper.LANG_INDONESIAN)
        assertEquals("id", localeId.language)
    }

    @Test
    fun `formatDigitsForSpeech formats phone numbers with spaces for digit by digit pronunciation`() {
        val input = "Please call my emergency contact: 08123456789"
        val expected = "Please call my emergency contact: 0 8 1 2 3 4 5 6 7 8 9"
        assertEquals(expected, LocaleHelper.formatDigitsForSpeech(input))

        val international = "Contact: +62 812-345-678"
        val formattedIntl = LocaleHelper.formatDigitsForSpeech(international)
        assertEquals("Contact: 6 2 8 1 2 - 3 4 5 - 6 7 8", formattedIntl)
    }

    @Test
    fun `formatDigitsForSpeech leaves short numbers untouched`() {
        val shortNumber = "Room 102 in Area 4"
        assertEquals(shortNumber, LocaleHelper.formatDigitsForSpeech(shortNumber))
    }
}

