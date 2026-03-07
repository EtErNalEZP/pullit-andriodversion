package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Step
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class CookingModeViewModel(application: Application) : AndroidViewModel(application) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _steps = MutableStateFlow<List<Step>>(emptyList())
    val steps: StateFlow<List<Step>> = _steps.asStateFlow()

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _recipeTitle = MutableStateFlow("")
    val recipeTitle: StateFlow<String> = _recipeTitle.asStateFlow()

    private var timerJob: Job? = null

    fun setup(title: String, stepsJson: String?, ingredientsJson: String?) {
        _recipeTitle.value = title
        _steps.value = stepsJson?.let {
            runCatching { json.decodeFromString<List<Step>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
        _ingredients.value = ingredientsJson?.let {
            runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
        _currentStepIndex.value = 0
        _timerSeconds.value = 0
        _isTimerRunning.value = false
    }

    fun nextStep() {
        if (_currentStepIndex.value < _steps.value.size - 1) _currentStepIndex.value++
    }

    fun previousStep() {
        if (_currentStepIndex.value > 0) _currentStepIndex.value--
    }

    fun goToStep(index: Int) {
        if (index in _steps.value.indices) _currentStepIndex.value = index
    }

    fun startTimer(seconds: Int) {
        _timerSeconds.value = seconds
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerSeconds.value--
            }
            _isTimerRunning.value = false
        }
    }

    fun pauseTimer() { _isTimerRunning.value = false; timerJob?.cancel() }

    fun resumeTimer() {
        if (_timerSeconds.value > 0) {
            _isTimerRunning.value = true
            timerJob = viewModelScope.launch {
                while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                    delay(1000)
                    _timerSeconds.value--
                }
                _isTimerRunning.value = false
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerSeconds.value = 0
        _isTimerRunning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
