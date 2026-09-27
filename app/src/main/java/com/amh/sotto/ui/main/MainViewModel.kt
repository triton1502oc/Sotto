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

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    // For backwards compatibility
    val customCategories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _voiceSettings = MutableStateFlow(VoiceSettings())
    val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

    init {
        _phrases.value = repository.getPhrases()
        _categories.value = repository.getCategories()
        _voiceSettings.value = voiceSettingsRepository.getVoiceSettings()
    }

    fun addCategory(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed.equals("ALL", ignoreCase = true)) return false
        val current = _categories.value.toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return false
        current.add(trimmed)
        _categories.value = current
        repository.saveCategories(current)
        return true
    }

    fun renameCategory(oldName: String, newName: String): Boolean {
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || trimmedNew.equals("ALL", ignoreCase = true)) return false
        if (Phrase.isUnrenamableCategory(oldName)) return false
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.equals(oldName, ignoreCase = true) }
        if (index == -1) return false
        if (current.any { it.equals(trimmedNew, ignoreCase = true) && !it.equals(oldName, ignoreCase = true) }) return false

        current[index] = trimmedNew
        _categories.value = current
        repository.saveCategories(current)

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

    fun deleteCategory(name: String): Boolean {
        if (Phrase.isUndeletableCategory(name)) return false
        val current = _categories.value.toMutableList()
        val removed = current.removeAll { it.equals(name, ignoreCase = true) }
        if (!removed) return false
        _categories.value = current
        repository.saveCategories(current)

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

    fun reorderCategories(fromIndex: Int, toIndex: Int) {
        val current = _categories.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _categories.value = current
            repository.saveCategories(current)
        }
    }

    fun moveCategory(fromName: String, toName: String) {
        val current = _categories.value.toMutableList()
        val from = current.indexOfFirst { it.equals(fromName, ignoreCase = true) }
        val to = current.indexOfFirst { it.equals(toName, ignoreCase = true) }
        if (from != -1 && to != -1 && from != to) {
            val item = current.removeAt(from)
            current.add(to, item)
            _categories.value = current
            repository.saveCategories(current)
        }
    }

    // Backwards compatibility methods
    fun addCustomCategory(name: String): Boolean = addCategory(name)
    fun renameCustomCategory(oldName: String, newName: String): Boolean = renameCategory(oldName, newName)
    fun deleteCustomCategory(name: String): Boolean = deleteCategory(name)

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
