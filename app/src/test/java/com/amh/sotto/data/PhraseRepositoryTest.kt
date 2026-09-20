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
}
