package com.amh.sotto.ui.main

import androidx.lifecycle.ViewModel
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.PhraseRepository
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.data.VoiceSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val repository: PhraseRepository,
    private val voiceSettingsRepository: VoiceSettingsRepository
) : ViewModel() {
    private val _phrases = MutableStateFlow<List<Phrase>>(emptyList())
    val phrases: StateFlow<List<Phrase>> = _phrases.asStateFlow()

    private val _voiceSettings = MutableStateFlow(VoiceSettings())
    val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

    init {
        _phrases.value = repository.getPhrases()
        _voiceSettings.value = voiceSettingsRepository.getVoiceSettings()
    }

    fun addPhrase(newPhrase: Phrase) {
        val currentList = _phrases.value.toMutableList()
        currentList.add(newPhrase)
        _phrases.value = currentList
        repository.savePhrases(currentList)
    }

    fun editPhrase(oldPhrase: Phrase, newPhrase: Phrase) {
        val currentList = _phrases.value.toMutableList()
        val index = currentList.indexOf(oldPhrase)
        if (index != -1) {
            currentList[index] = newPhrase
            _phrases.value = currentList
            repository.savePhrases(currentList)
        }
    }

    fun deletePhrase(phrase: Phrase) {
        val currentList = _phrases.value.toMutableList()
        if (currentList.remove(phrase)) {
            _phrases.value = currentList
            repository.savePhrases(currentList)
        }
    }

    fun movePhrase(from: Int, to: Int) {
        val currentList = _phrases.value.toMutableList()
        if (from in 0 until currentList.size && to in 0 until currentList.size) {
            val item = currentList.removeAt(from)
            currentList.add(to, item)
            _phrases.value = currentList
            repository.savePhrases(currentList)
        }
    }

    fun movePhrase(fromPhrase: Phrase, toPhrase: Phrase) {
        val currentList = _phrases.value.toMutableList()
        val from = currentList.indexOf(fromPhrase)
        val to = currentList.indexOf(toPhrase)
        if (from != -1 && to != -1 && from != to) {
            val item = currentList.removeAt(from)
            currentList.add(to, item)
            _phrases.value = currentList
            repository.savePhrases(currentList)
        }
    }

    fun updateVoiceSettings(newSettings: VoiceSettings) {
        _voiceSettings.value = newSettings
        voiceSettingsRepository.saveVoiceSettings(newSettings)
    }
}
