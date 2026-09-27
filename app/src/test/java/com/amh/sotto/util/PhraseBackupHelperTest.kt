package com.amh.sotto.util

import com.amh.sotto.data.Phrase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseBackupHelperTest {

    private val samplePhrases = listOf(
        Phrase("I cannot speak right now. Please read my screen.", isEmergency = true, category = Phrase.CATEGORY_EMERGENCY),
        Phrase("I need a quiet space.", category = Phrase.CATEGORY_NEEDS),
        Phrase("Thank you.", spokenText = "Terima kasih", spokenLanguage = "id", category = Phrase.CATEGORY_SOCIAL),
        Phrase("Hello.", category = Phrase.CATEGORY_GENERAL)
    )

    private val sampleCategories = listOf(
        Phrase.CATEGORY_EMERGENCY,
        Phrase.CATEGORY_NEEDS,
        Phrase.CATEGORY_SOCIAL,
        Phrase.CATEGORY_CARE,
        Phrase.CATEGORY_GENERAL
    )

    @Test
    fun `exportToJson creates valid JSON containing version, categories, and phrases`() {
        val json = PhraseBackupHelper.exportToJson(samplePhrases, sampleCategories)
        assertNotNull(json)
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"categories\""))
        assertTrue(json.contains("\"phrases\""))
        assertTrue(json.contains("Terima kasih"))
    }

    @Test
    fun `parseBackupJson successfully parses structured JSON backup`() {
        val json = PhraseBackupHelper.exportToJson(samplePhrases, sampleCategories)
        val backupData = PhraseBackupHelper.parseBackupJson(json)

        assertEquals(1, backupData.version)
        assertEquals(4, backupData.phrases.size)
        assertEquals("I cannot speak right now. Please read my screen.", backupData.phrases[0].text)
        assertTrue(backupData.phrases[0].isEmergency)
        assertEquals(Phrase.CATEGORY_EMERGENCY, backupData.phrases[0].category)

        assertEquals("Thank you.", backupData.phrases[2].text)
        assertEquals("Terima kasih", backupData.phrases[2].spokenText)
        assertEquals("id", backupData.phrases[2].spokenLanguage)
        assertEquals(Phrase.CATEGORY_SOCIAL, backupData.phrases[2].category)

        assertTrue(backupData.categories.contains(Phrase.CATEGORY_EMERGENCY))
        assertTrue(backupData.categories.contains(Phrase.CATEGORY_GENERAL))
    }

    @Test
    fun `parseBackupJson handles legacy phrase array format`() {
        val legacyJson = """
            [
                {"text": "Help me", "isEmergency": true, "category": "Emergency"},
                {"text": "Water please", "category": "Needs"},
                "Plain string phrase"
            ]
        """.trimIndent()

        val backupData = PhraseBackupHelper.parseBackupJson(legacyJson)
        assertEquals(3, backupData.phrases.size)
        assertEquals("Help me", backupData.phrases[0].text)
        assertTrue(backupData.phrases[0].isEmergency)
        assertEquals("Plain string phrase", backupData.phrases[2].text)
        assertEquals(Phrase.CATEGORY_GENERAL, backupData.phrases[2].category)
        assertTrue(backupData.categories.contains(Phrase.CATEGORY_EMERGENCY))
        assertTrue(backupData.categories.contains(Phrase.CATEGORY_GENERAL))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseBackupJson throws IllegalArgumentException on empty string`() {
        PhraseBackupHelper.parseBackupJson("   ")
    }

    @Test(expected = Exception::class)
    fun `parseBackupJson throws on malformed JSON`() {
        PhraseBackupHelper.parseBackupJson("{ not valid json }")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseBackupJson throws when phrases list is empty`() {
        PhraseBackupHelper.parseBackupJson("""{"version": 1, "categories": ["General"], "phrases": []}""")
    }

    @Test
    fun `mergeData appends non-duplicate phrases and missing categories`() {
        val currentPhrases = listOf(
            Phrase("Existing phrase", category = Phrase.CATEGORY_GENERAL),
            Phrase("I need help", isEmergency = true, category = Phrase.CATEGORY_EMERGENCY)
        )
        val currentCategories = listOf(Phrase.CATEGORY_EMERGENCY, Phrase.CATEGORY_GENERAL)

        val backup = BackupData(
            version = 1,
            categories = listOf(Phrase.CATEGORY_EMERGENCY, "Work", Phrase.CATEGORY_GENERAL),
            phrases = listOf(
                Phrase("Existing phrase", category = Phrase.CATEGORY_GENERAL), // Duplicate
                Phrase("New work phrase", category = "Work") // New
            )
        )

        val (mergedPhrases, mergedCategories) = PhraseBackupHelper.mergeData(
            currentPhrases = currentPhrases,
            currentCategories = currentCategories,
            backup = backup
        )

        assertEquals(3, mergedPhrases.size)
        assertTrue(mergedPhrases.any { it.text == "New work phrase" })
        assertTrue(mergedCategories.contains("Work"))
    }

    @Test
    fun `replaceData replaces phrases and categories while preserving emergency phrase`() {
        val currentPhrases = listOf(
            Phrase("Old phrase", category = Phrase.CATEGORY_GENERAL),
            Phrase("Emergency message", isEmergency = true, category = Phrase.CATEGORY_EMERGENCY)
        )

        val backupWithoutEmergency = BackupData(
            version = 1,
            categories = listOf("Work", Phrase.CATEGORY_GENERAL),
            phrases = listOf(Phrase("Imported phrase", category = "Work"))
        )

        val (newPhrases, newCategories) = PhraseBackupHelper.replaceData(
            currentPhrases = currentPhrases,
            backup = backupWithoutEmergency
        )

        // Preserves existing emergency phrase if backup has none
        assertTrue(newPhrases.any { it.isEmergency })
        assertTrue(newPhrases.any { it.text == "Imported phrase" })
        assertFalse(newPhrases.any { it.text == "Old phrase" })

        // Guaranteed essential categories
        assertTrue(newCategories.contains(Phrase.CATEGORY_EMERGENCY))
        assertTrue(newCategories.contains(Phrase.CATEGORY_GENERAL))
        assertTrue(newCategories.contains("Work"))
    }
}
