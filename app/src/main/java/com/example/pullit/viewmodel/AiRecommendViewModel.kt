package com.example.pullit.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.*
import com.example.pullit.service.IngredientRecognitionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

enum class AiState { IDLE, ANALYZING, GENERATING, READY, ERROR }

class AiRecommendViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val recipeDao = db.recipeDao()
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(AiState.IDLE)
    val state: StateFlow<AiState> = _state.asStateFlow()

    private val _recognizedIngredients = MutableStateFlow<List<RecognizedIngredient>>(emptyList())
    val recognizedIngredients: StateFlow<List<RecognizedIngredient>> = _recognizedIngredients.asStateFlow()

    private val _editableIngredients = MutableStateFlow<List<String>>(emptyList())
    val editableIngredients: StateFlow<List<String>> = _editableIngredients.asStateFlow()

    private val _suggestions = MutableStateFlow<List<RecipeSuggestion>>(emptyList())
    val suggestions: StateFlow<List<RecipeSuggestion>> = _suggestions.asStateFlow()

    private val _localMatches = MutableStateFlow<List<RecipeRecommendation>>(emptyList())
    val localMatches: StateFlow<List<RecipeRecommendation>> = _localMatches.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun analyzeImage(imageData: ByteArray) = viewModelScope.launch {
        _state.value = AiState.ANALYZING
        _error.value = null
        try {
            val base64 = Base64.encodeToString(imageData, Base64.NO_WRAP)
            val ingredients = IngredientRecognitionService.recognizeIngredients(base64)
            _recognizedIngredients.value = ingredients
            _editableIngredients.value = ingredients.map { it.name }

            val allRecipes = recipeDao.getAll().first()
            _localMatches.value = IngredientRecognitionService.matchLocalRecipes(
                ingredients.map { it.name }, allRecipes
            )
            _state.value = AiState.READY
        } catch (e: Exception) {
            _error.value = e.message
            _state.value = AiState.ERROR
        }
    }

    fun addIngredient(name: String) {
        if (name.isNotBlank()) _editableIngredients.value = _editableIngredients.value + name
    }

    fun removeIngredient(index: Int) {
        _editableIngredients.value = _editableIngredients.value.toMutableList().also { it.removeAt(index) }
    }

    fun generateSuggestions() = viewModelScope.launch {
        _state.value = AiState.GENERATING
        try {
            val suggestions = IngredientRecognitionService.suggestRecipes(_editableIngredients.value)
            _suggestions.value = suggestions
            _state.value = AiState.READY
        } catch (e: Exception) {
            _error.value = e.message
            _state.value = AiState.ERROR
        }
    }

    fun saveRecipeFromSuggestion(suggestion: RecipeSuggestion) = viewModelScope.launch {
        val ingredientsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Ingredient.serializer()),
            suggestion.ingredients.map { Ingredient(name = it) }
        )
        val stepsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Step.serializer()),
            suggestion.steps.mapIndexed { i, s -> Step(order = i + 1, instruction = s) }
        )
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = suggestion.title,
            desc = suggestion.description,
            calories = suggestion.calories,
            ingredientsJson = ingredientsJson,
            stepsJson = stepsJson
        )
        recipeDao.upsert(recipe)
    }

    fun reset() {
        _state.value = AiState.IDLE
        _recognizedIngredients.value = emptyList()
        _editableIngredients.value = emptyList()
        _suggestions.value = emptyList()
        _localMatches.value = emptyList()
        _error.value = null
    }
}
