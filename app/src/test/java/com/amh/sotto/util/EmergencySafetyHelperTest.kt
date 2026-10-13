package com.amh.sotto.util

import android.content.Context
import com.amh.sotto.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class EmergencySafetyHelperTest {

    @Test
    fun `hasUnresolvedPlaceholders returns true when bracket placeholders present`() {
        val textEn = "I live around [Area/Neighborhood], please call my emergency contact: [phone number]"
        val textId = "Saya tinggal di sekitar [Area / Lingkungan], tolong hubungi kontak darurat saya: [nomor telepon]"
        val resolved = "I live around Brooklyn, please call my emergency contact: 555-1234"

        assertTrue(EmergencySafetyHelper.hasUnresolvedPlaceholders(textEn))
        assertTrue(EmergencySafetyHelper.hasUnresolvedPlaceholders(textId))
        assertFalse(EmergencySafetyHelper.hasUnresolvedPlaceholders(resolved))
    }

    @Test
    fun `cleanPlaceholdersForSpeech removes brackets and placeholders for speech`() {
        val text = "I live around [Area/Neighborhood], please call my emergency contact: [phone number]"
        val cleaned = EmergencySafetyHelper.cleanPlaceholdersForSpeech(text)

        assertFalse(cleaned.contains("["))
        assertFalse(cleaned.contains("]"))
        assertFalse(cleaned.contains("Area/Neighborhood"))
        assertTrue(cleaned.startsWith("I live around"))
    }

    @Test
    fun `cleanPlaceholdersForSpeech returns original text when no placeholders present`() {
        val text = "Please call 911 immediately"
        val cleaned = EmergencySafetyHelper.cleanPlaceholdersForSpeech(text)
        assertEquals(text, cleaned)
    }

    @Test
    fun `formatSafetyPhrase formats display and spoken text with natural digits`() {
        val context = mockk<Context>()
        every {
            context.getString(
                R.string.emergency_contact_phrase_format,
                "South Jakarta",
                " (Mom)",
                "08123456789"
            )
        } returns "I live around South Jakarta, please call my emergency contact (Mom): 08123456789"

        val (display, spoken) = EmergencySafetyHelper.formatSafetyPhrase(
            context = context,
            area = "South Jakarta",
            contactName = "Mom",
            phone = "08123456789",
            locale = Locale.US
        )

        assertEquals("I live around South Jakarta, please call my emergency contact (Mom): 08123456789", display)
        assertFalse(EmergencySafetyHelper.hasUnresolvedPlaceholders(display))
        assertTrue(spoken.contains("0 8 1 2 3 4 5 6 7 8 9"))
    }
}
