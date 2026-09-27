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
        every { repository.getCategories() } returns Phrase.DEFAULT_CATEGORIES
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
    fun `addCategory adds valid category and saves to repository`() {
        val result = viewModel.addCategory("Food")
        assertTrue(result)
        val expected = Phrase.DEFAULT_CATEGORIES + "Food"
        assertEquals(expected, viewModel.categories.value)
        verify { repository.saveCategories(expected) }
    }

    @Test
    fun `addCategory rejects blank, duplicate, or reserved category names`() {
        assertFalse(viewModel.addCategory(""))
        assertFalse(viewModel.addCategory("   "))
        assertFalse(viewModel.addCategory("ALL"))
        assertFalse(viewModel.addCategory("Emergency")) // already exists in defaults
        assertFalse(viewModel.addCategory("Care")) // already exists in defaults

        assertTrue(viewModel.addCategory("Food"))
        assertFalse(viewModel.addCategory("food"))
        assertFalse(viewModel.addCategory("Food"))
    }

    @Test
    fun `renameCategory renames category and updates affected phrases`() {
        viewModel.addCategory("Food")
        val phraseInFood = Phrase("I want noodles", "en", category = "Food")
        viewModel.addPhrase(phraseInFood)

        val result = viewModel.renameCategory("Food", "Meals")
        assertTrue(result)
        assertTrue(viewModel.categories.value.contains("Meals"))
        assertFalse(viewModel.categories.value.contains("Food"))

        val updatedPhrase = viewModel.phrases.value.find { it.text == "I want noodles" }
        assertEquals("Meals", updatedPhrase?.category)
        verify { repository.saveCategories(any()) }
    }

    @Test
    fun `renameCategory rejects renaming Emergency or ALL`() {
        assertFalse(viewModel.renameCategory("Emergency", "Urgent"))
        assertFalse(viewModel.renameCategory("ALL", "Everything"))
    }

    @Test
    fun `deleteCategory removes category and safely moves phrases to General`() {
        viewModel.addCategory("Food")
        val phraseInFood = Phrase("I want noodles", "en", category = "Food")
        viewModel.addPhrase(phraseInFood)

        val result = viewModel.deleteCategory("Food")
        assertTrue(result)
        assertFalse(viewModel.categories.value.contains("Food"))

        val movedPhrase = viewModel.phrases.value.find { it.text == "I want noodles" }
        assertEquals(Phrase.CATEGORY_GENERAL, movedPhrase?.category)
        verify { repository.saveCategories(any()) }
    }

    @Test
    fun `deleteCategory rejects deleting Emergency or General`() {
        assertFalse(viewModel.deleteCategory("Emergency"))
        assertFalse(viewModel.deleteCategory("General"))
        assertFalse(viewModel.deleteCategory("ALL"))
    }

    @Test
    fun `deleteCategory allows deleting Care, Needs, or Social`() {
        val carePhrase = Phrase("Are you hurting?", "en", category = Phrase.CATEGORY_CARE)
        viewModel.addPhrase(carePhrase)

        val result = viewModel.deleteCategory(Phrase.CATEGORY_CARE)
        assertTrue(result)
        assertFalse(viewModel.categories.value.contains(Phrase.CATEGORY_CARE))

        val movedCarePhrase = viewModel.phrases.value.find { it.text == "Are you hurting?" }
        assertEquals(Phrase.CATEGORY_GENERAL, movedCarePhrase?.category)
    }

    @Test
    fun `reorderCategories reorders categories and saves to repository`() {
        val initialOrder = viewModel.categories.value
        val first = initialOrder[0]
        val second = initialOrder[1]

        viewModel.reorderCategories(0, 1)

        val newOrder = viewModel.categories.value
        assertEquals(second, newOrder[0])
        assertEquals(first, newOrder[1])
        verify { repository.saveCategories(newOrder) }
    }
}
