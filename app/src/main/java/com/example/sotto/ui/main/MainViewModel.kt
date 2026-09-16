package com.example.sotto.ui.main

import androidx.lifecycle.ViewModel
import com.example.sotto.data.PhraseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val repository: PhraseRepository) : ViewModel() {
    private val _phrases = MutableStateFlow<List<String>>(emptyList())
    val phrases: StateFlow<List<String>> = _phrases.asStateFlow()

    init {
        _phrases.value = repository.getPhrases()
    }

    fun addPhrase(newPhrase: String) {
        val currentList = _phrases.value.toMutableList()
        currentList.add(newPhrase)
        _phrases.value = currentList
        repository.savePhrases(currentList)
    }

    fun editPhrase(oldPhrase: String, newPhrase: String) {
        val currentList = _phrases.value.toMutableList()
        val index = currentList.indexOf(oldPhrase)
        if (index != -1) {
            currentList[index] = newPhrase
            _phrases.value = currentList
            repository.savePhrases(currentList)
        }
    }

    fun deletePhrase(phrase: String) {
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
}
