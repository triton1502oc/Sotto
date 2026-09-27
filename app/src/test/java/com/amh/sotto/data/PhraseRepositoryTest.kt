package com.amh.sotto.data

import com.amh.sotto.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseRepositoryTest {

    @Test
    fun `Phrase constructor sets language as second parameter and defaults spokenText to null`() {
        val phrase = Phrase("Hello", "en")
        assertEquals("Hello", phrase.text)
        assertEquals("en", phrase.language)
        assertNull(phrase.spokenText)
        assertNull(phrase.spokenLanguage)
        assertFalse(phrase.isEmergency)
        assertEquals(Phrase.CATEGORY_GENERAL, phrase.category)
    }

    @Test
    fun `Phrase constructor with auto language sets language to auto and spokenText to null`() {
        val phrase = Phrase("Quick text", LocaleHelper.LANG_AUTO)
        assertEquals("Quick text", phrase.text)
        assertEquals(LocaleHelper.LANG_AUTO, phrase.language)
        assertNull(phrase.spokenText)
        assertNull(phrase.spokenLanguage)
    }

    @Test
    fun `Phrase with custom spokenText and spokenLanguage retains all fields`() {
        val phrase = Phrase(
            text = "Thank you",
            language = "en",
            spokenText = "Terima kasih",
            spokenLanguage = "id",
            isEmergency = false,
            category = Phrase.CATEGORY_SOCIAL
        )
        assertEquals("Thank you", phrase.text)
        assertEquals("en", phrase.language)
        assertEquals("Terima kasih", phrase.spokenText)
        assertEquals("id", phrase.spokenLanguage)
        assertFalse(phrase.isEmergency)
        assertEquals(Phrase.CATEGORY_SOCIAL, phrase.category)
    }

    @Test
    fun `Phrase copy correctly updates spokenText and spokenLanguage`() {
        val original = Phrase("Where is the restroom?", "en")
        val updated = original.copy(spokenText = "Di mana kamar mandi?", spokenLanguage = "id")

        assertEquals("Where is the restroom?", updated.text)
        assertEquals("en", updated.language)
        assertEquals("Di mana kamar mandi?", updated.spokenText)
        assertEquals("id", updated.spokenLanguage)
    }

    @Test
    fun `Phrase emergency category sets emergency correctly`() {
        val emergencyPhrase = Phrase(
            text = "I cannot speak right now. Please read my screen.",
            language = LocaleHelper.LANG_AUTO,
            isEmergency = true,
            category = Phrase.CATEGORY_EMERGENCY
        )
        assertTrue(emergencyPhrase.isEmergency)
        assertEquals(Phrase.CATEGORY_EMERGENCY, emergencyPhrase.category)
    }

    @Test
    fun `Phrase care category sets category correctly`() {
        val carePhrase = Phrase(
            text = "Are you in pain?",
            language = LocaleHelper.LANG_AUTO,
            isEmergency = false,
            category = Phrase.CATEGORY_CARE
        )
        assertFalse(carePhrase.isEmergency)
        assertEquals(Phrase.CATEGORY_CARE, carePhrase.category)
    }

    @Test
    fun `Phrase isSystemCategory identifies protected system categories and ALL correctly`() {
        assertTrue(Phrase.isSystemCategory("ALL"))
        assertTrue(Phrase.isSystemCategory("all"))
        assertTrue(Phrase.isSystemCategory("Emergency"))
        assertTrue(Phrase.isSystemCategory("emergency"))
        assertTrue(Phrase.isSystemCategory("General"))
        assertTrue(Phrase.isSystemCategory("general"))

        // Demoted categories are no longer protected system categories
        assertFalse(Phrase.isSystemCategory("Care"))
        assertFalse(Phrase.isSystemCategory("Needs"))
        assertFalse(Phrase.isSystemCategory("Social"))
    }

    @Test
    fun `Phrase isUndeletableCategory protects ALL, Emergency, and General`() {
        assertTrue(Phrase.isUndeletableCategory("ALL"))
        assertTrue(Phrase.isUndeletableCategory("Emergency"))
        assertTrue(Phrase.isUndeletableCategory("General"))

        assertFalse(Phrase.isUndeletableCategory("Needs"))
        assertFalse(Phrase.isUndeletableCategory("Social"))
        assertFalse(Phrase.isUndeletableCategory("Care"))
        assertFalse(Phrase.isUndeletableCategory("Food"))
    }

    @Test
    fun `Phrase isUnrenamableCategory protects ALL and Emergency only`() {
        assertTrue(Phrase.isUnrenamableCategory("ALL"))
        assertTrue(Phrase.isUnrenamableCategory("Emergency"))

        assertFalse(Phrase.isUnrenamableCategory("General"))
        assertFalse(Phrase.isUnrenamableCategory("Needs"))
        assertFalse(Phrase.isUnrenamableCategory("Social"))
        assertFalse(Phrase.isUnrenamableCategory("Care"))
        assertFalse(Phrase.isUnrenamableCategory("Food"))
    }

    @Test
    fun `Phrase isSystemCategory returns false for custom category names`() {
        assertFalse(Phrase.isSystemCategory("Food"))
        assertFalse(Phrase.isSystemCategory("Medical"))
        assertFalse(Phrase.isSystemCategory("Places"))
    }
}
