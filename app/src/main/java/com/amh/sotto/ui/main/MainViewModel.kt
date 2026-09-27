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

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _voiceSettings = MutableStateFlow(VoiceSettings())
    val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

    init {
        _phrases.value = repository.getPhrases()
        _customCategories.value = repository.getCustomCategories()
        _voiceSettings.value = voiceSettingsRepository.getVoiceSettings()
    }

    fun addCustomCategory(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank() || Phrase.isSystemCategory(trimmed)) return false
        val current = _customCategories.value.toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return false
        current.add(trimmed)
        _customCategories.value = current
        repository.saveCustomCategories(current)
        return true
    }

    fun renameCustomCategory(oldName: String, newName: String): Boolean {
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || Phrase.isSystemCategory(trimmedNew)) return false
        val current = _customCategories.value.toMutableList()
        val index = current.indexOfFirst { it.equals(oldName, ignoreCase = true) }
        if (index == -1) return false
        if (current.any { it.equals(trimmedNew, ignoreCase = true) && !it.equals(oldName, ignoreCase = true) }) return false

        current[index] = trimmedNew
        _customCategories.value = current
        repository.saveCustomCategories(current)

        val updatedPhrases = _phrases.value.map { phrase ->
            if (phrase.category.equals(oldName, ignoreCase = true)) {
                phrase.copy(category = trimmedNew)
            } else {
                phrase
            }
        }
        _phrases.value = updatedPhrases
        repository.savePhrases(updatedPhrases)
        return true
    }

    fun deleteCustomCategory(name: String): Boolean {
        if (Phrase.isSystemCategory(name)) return false
        val current = _customCategories.value.toMutableList()
        val removed = current.removeAll { it.equals(name, ignoreCase = true) }
        if (!removed) return false
        _customCategories.value = current
        repository.saveCustomCategories(current)

        val updatedPhrases = _phrases.value.map { phrase ->
            if (phrase.category.equals(name, ignoreCase = true)) {
                phrase.copy(category = Phrase.CATEGORY_GENERAL)
            } else {
                phrase
            }
        }
        _phrases.value = updatedPhrases
        repository.savePhrases(updatedPhrases)
        return true
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
