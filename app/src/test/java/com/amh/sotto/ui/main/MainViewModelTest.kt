package com.amh.sotto.ui.main

import com.amh.sotto.data.Phrase
import com.amh.sotto.data.PhraseRepository
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.data.VoiceSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
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
}
