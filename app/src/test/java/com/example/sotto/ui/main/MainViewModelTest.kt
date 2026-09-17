package com.example.sotto.ui.main

import com.example.sotto.data.PhraseRepository
import com.example.sotto.data.VoiceSettings
import com.example.sotto.data.VoiceSettingsRepository
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

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        voiceSettingsRepository = mockk(relaxed = true)
        every { repository.getPhrases() } returns listOf("Phrase 1", "Phrase 2", "Phrase 3")
        every { voiceSettingsRepository.getVoiceSettings() } returns VoiceSettings(speechRate = 1.0f, speechPitch = 1.0f)
        viewModel = MainViewModel(repository, voiceSettingsRepository)
    }

    @Test
    fun `initial state loads phrases and voice settings from repository`() {
        assertEquals(listOf("Phrase 1", "Phrase 2", "Phrase 3"), viewModel.phrases.value)
        assertEquals(VoiceSettings(speechRate = 1.0f, speechPitch = 1.0f), viewModel.voiceSettings.value)
        verify { repository.getPhrases() }
        verify { voiceSettingsRepository.getVoiceSettings() }
    }

    @Test
    fun `addPhrase appends phrase and saves to repository`() {
        viewModel.addPhrase("Phrase 4")

        val expected = listOf("Phrase 1", "Phrase 2", "Phrase 3", "Phrase 4")
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `editPhrase updates existing phrase and saves to repository`() {
        viewModel.editPhrase("Phrase 2", "Updated Phrase 2")

        val expected = listOf("Phrase 1", "Updated Phrase 2", "Phrase 3")
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `deletePhrase removes phrase and saves to repository`() {
        viewModel.deletePhrase("Phrase 2")

        val expected = listOf("Phrase 1", "Phrase 3")
        assertEquals(expected, viewModel.phrases.value)
        verify { repository.savePhrases(expected) }
    }

    @Test
    fun `movePhrase reorders list and saves to repository`() {
        viewModel.movePhrase(0, 2)

        val expected = listOf("Phrase 2", "Phrase 3", "Phrase 1")
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
