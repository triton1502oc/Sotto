package com.amh.sotto.ui.main

import com.amh.sotto.data.Phrase
import com.amh.sotto.data.PhraseRepository
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.data.VoiceSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var repository: PhraseRepository
    private lateinit var voiceSettingsRepository: VoiceSettingsRepository
    private lateinit var viewModel: MainViewModel

    private val initialPhrases = listOf(
        Phrase("Phrase 1", "auto"),
        Phrase("Phrase 2", "en"),
        Phrase("Phrase 3", "id")
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        voiceSettingsRepository = mockk(relaxed = true)
        every { repository.getPhrases() } returns initialPhrases
        every { voiceSettingsRepository.getVoiceSettings() } returns VoiceSettings(speechRate = 1.0f, speechPitch = 1.0f)
        viewModel = MainViewModel(repository, voiceSettingsRepository)
    }

    @Test
    fun `initial state loads phrases and voice settings from repository`() {
        assertEquals(initialPhrases, viewModel.phrases.value)
        assertEquals(VoiceSettings(speechRate = 1.0f, speechPitch = 1.0f), viewModel.voiceSettings.value)
        verify { repository.getPhrases() }
        verify { voiceSettingsRepository.getVoiceSettings() }
    }

    @Test
    fun `addPhrase appends phrase and saves to repository`() {
        val newPhrase = Phrase("Phrase 4", "auto")
        viewModel.addPhrase(newPhrase)

        val expected = listOf(
            Phrase("Phrase 1", "auto"),
            Phrase("Phrase 2", "en"),
            Phrase("Phrase 3", "id"),
            newPhrase
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `addPhrase with spokenText and spokenLanguage saves correctly`() {
        val bilingualPhrase = Phrase(
            text = "Thank you",
            spokenText = "Terima kasih",
            spokenLanguage = "id",
            category = Phrase.CATEGORY_SOCIAL
        )
        viewModel.addPhrase(bilingualPhrase)

        val expected = listOf(
            Phrase("Phrase 1", "auto"),
            Phrase("Phrase 2", "en"),
            Phrase("Phrase 3", "id"),
            bilingualPhrase
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `editPhrase updates existing phrase and saves to repository`() {
        val oldPhrase = Phrase("Phrase 2", "en")
        val updatedPhrase = Phrase("Updated Phrase 2", "id")
        viewModel.editPhrase(oldPhrase, updatedPhrase)

        val expected = listOf(
            Phrase("Phrase 1", "auto"),
            updatedPhrase,
            Phrase("Phrase 3", "id")
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `editPhrase updates phrase with spokenText`() {
        val oldPhrase = Phrase("Phrase 2", "en")
        val updatedPhrase = Phrase("Where is the restroom?", spokenText = "Di mana kamar mandi?", spokenLanguage = "id")
        viewModel.editPhrase(oldPhrase, updatedPhrase)

        val expected = listOf(
            Phrase("Phrase 1", "auto"),
            updatedPhrase,
            Phrase("Phrase 3", "id")
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `deletePhrase removes phrase and saves to repository`() {
        val phraseToDelete = Phrase("Phrase 2", "en")
        viewModel.deletePhrase(phraseToDelete)

        val expected = listOf(
            Phrase("Phrase 1", "auto"),
            Phrase("Phrase 3", "id")
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `movePhrase reorders list and saves to repository`() {
        viewModel.movePhrase(0, 2)

        val expected = listOf(
            Phrase("Phrase 2", "en"),
            Phrase("Phrase 3", "id"),
            Phrase("Phrase 1", "auto")
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `movePhrase by Phrase object reorders list and saves to repository`() {
        val phrase1 = initialPhrases[0]
        val phrase3 = initialPhrases[2]
        viewModel.movePhrase(phrase1, phrase3)

        val expected = listOf(
            Phrase("Phrase 2", "en"),
            Phrase("Phrase 3", "id"),
            Phrase("Phrase 1", "auto")
        )
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `updateVoiceSettings updates state and saves to repository`() {
        val newSettings = VoiceSettings(speechRate = 0.8f, speechPitch = 1.1f, voiceName = "test_voice")
        viewModel.updateVoiceSettings(newSettings)

        assertEquals(newSettings, viewModel.voiceSettings.value)
        verify { voiceSettingsRepository.saveVoiceSettings(newSettings) }
    }

    @Test
    fun `addCustomCategory adds valid category and saves to repository`() {
        val result = viewModel.addCustomCategory("Food")
        assertTrue(result)
        assertEquals(listOf("Food"), viewModel.customCategories.value)
        verify { repository.saveCustomCategories(listOf("Food")) }
    }

    @Test
    fun `addCustomCategory rejects blank, duplicate, or system category names`() {
        assertFalse(viewModel.addCustomCategory(""))
        assertFalse(viewModel.addCustomCategory("   "))
        assertFalse(viewModel.addCustomCategory("Emergency"))
        assertFalse(viewModel.addCustomCategory("care"))
        assertFalse(viewModel.addCustomCategory("ALL"))
        assertFalse(viewModel.addCustomCategory("General"))

        assertTrue(viewModel.addCustomCategory("Food"))
        assertFalse(viewModel.addCustomCategory("food"))
        assertFalse(viewModel.addCustomCategory("Food"))
    }

    @Test
    fun `renameCustomCategory renames category and updates affected phrases`() {
        viewModel.addCustomCategory("Food")
        val phraseInFood = Phrase("I want noodles", "en", category = "Food")
        viewModel.addPhrase(phraseInFood)

        val result = viewModel.renameCustomCategory("Food", "Meals")
        assertTrue(result)
        assertEquals(listOf("Meals"), viewModel.customCategories.value)

        val updatedPhrase = viewModel.phrases.value.find { it.text == "I want noodles" }
        assertEquals("Meals", updatedPhrase?.category)
        verify { repository.saveCustomCategories(listOf("Meals")) }
    }

    @Test
    fun `deleteCustomCategory removes category and safely moves phrases to General`() {
        viewModel.addCustomCategory("Food")
        val phraseInFood = Phrase("I want noodles", "en", category = "Food")
        viewModel.addPhrase(phraseInFood)

        val result = viewModel.deleteCustomCategory("Food")
        assertTrue(result)
        assertTrue(viewModel.customCategories.value.isEmpty())

        val movedPhrase = viewModel.phrases.value.find { it.text == "I want noodles" }
        assertEquals(Phrase.CATEGORY_GENERAL, movedPhrase?.category)
        verify { repository.saveCustomCategories(emptyList()) }
    }
}
