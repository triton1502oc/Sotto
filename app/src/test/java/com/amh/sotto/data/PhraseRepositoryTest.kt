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
            text = "I am in pain.",
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

    @Test
    fun `Phrase CATEGORY_SAFETY alias matches CATEGORY_EMERGENCY and is protected`() {
        assertEquals(Phrase.CATEGORY_EMERGENCY, Phrase.CATEGORY_SAFETY)
        assertTrue(Phrase.isSystemCategory(Phrase.CATEGORY_SAFETY))
        assertTrue(Phrase.isUndeletableCategory(Phrase.CATEGORY_SAFETY))
        assertTrue(Phrase.isUnrenamableCategory(Phrase.CATEGORY_SAFETY))
    }

    @Test
    fun `Safety default template is valid emergency phrase`() {
        val safetyPhrase = Phrase(
            text = "I live around [Area/Neighborhood], please call my emergency contact: [phone number]",
            isEmergency = true,
            category = Phrase.CATEGORY_SAFETY
        )

        assertTrue(safetyPhrase.isEmergency)
        assertEquals(Phrase.CATEGORY_EMERGENCY, safetyPhrase.category)
    }

    @Test
    fun `migrateCareRoleClarityPhrases migrates unmodified 2nd-person Care phrases to 1st-person and preserves custom care phrases`() {
        val inputPhrases = listOf(
            Phrase("Emergency contact", LocaleHelper.LANG_AUTO, isEmergency = true, category = Phrase.CATEGORY_EMERGENCY),
            Phrase("Are you in pain?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Do you need water or food?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Do you need the restroom?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Are you feeling cold or warm?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Do you want to rest or sleep?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Apakah merasa sakit?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Apakah butuh minum atau makan?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Apakah perlu ke kamar mandi?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Apakah merasa dingin atau panas?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Apakah ingin istirahat atau tidur?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Check pulse every 2 hours", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Are you in pain?", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_GENERAL)
        )

        val (migratedPhrases, changed) = SharedPreferencesPhraseRepository.migrateCareRoleClarityPhrases(inputPhrases)

        assertTrue(changed)

        // Emergency phrase unaffected
        assertEquals("Emergency contact", migratedPhrases[0].text)

        // English 2nd-person Care phrases migrated to 1st-person self-advocacy
        assertEquals("I am in pain.", migratedPhrases[1].text)
        assertEquals("I need water or food.", migratedPhrases[2].text)
        assertEquals("I need the restroom.", migratedPhrases[3].text)
        assertEquals("I feel too cold or too warm.", migratedPhrases[4].text)
        assertEquals("I need to rest or sleep.", migratedPhrases[5].text)

        // Indonesian 2nd-person Care phrases migrated to 1st-person self-advocacy
        assertEquals("Saya merasa sakit.", migratedPhrases[6].text)
        assertEquals("Saya butuh minum atau makan.", migratedPhrases[7].text)
        assertEquals("Saya perlu ke kamar mandi.", migratedPhrases[8].text)
        assertEquals("Saya merasa terlalu dingin atau panas.", migratedPhrases[9].text)
        assertEquals("Saya ingin istirahat atau tidur.", migratedPhrases[10].text)

        // Custom Care phrase preserved intact
        assertEquals("Check pulse every 2 hours", migratedPhrases[11].text)
        assertEquals(Phrase.CATEGORY_CARE, migratedPhrases[11].category)

        // Non-care category with same text unaffected
        assertEquals("Are you in pain?", migratedPhrases[12].text)
        assertEquals(Phrase.CATEGORY_GENERAL, migratedPhrases[12].category)
    }

    @Test
    fun `migrateCareRoleClarityPhrases returns false changed when phrases are already 1st-person or custom`() {
        val inputPhrases = listOf(
            Phrase("I am in pain.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Saya butuh minum atau makan.", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE),
            Phrase("Check pulse every 2 hours", LocaleHelper.LANG_AUTO, category = Phrase.CATEGORY_CARE)
        )

        val (migratedPhrases, changed) = SharedPreferencesPhraseRepository.migrateCareRoleClarityPhrases(inputPhrases)

        assertFalse(changed)
        assertEquals(inputPhrases, migratedPhrases)
    }
}
