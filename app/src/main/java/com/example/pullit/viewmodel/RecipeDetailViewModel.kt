package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.Cookbook
import com.example.pullit.data.model.MealPlanItem
import com.example.pullit.data.model.Recipe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val recipeDao = db.recipeDao()
    private val mealPlanDao = db.mealPlanDao()
    private val cookbookDao = db.cookbookDao()
    private val json = Json { ignoreUnknownKeys = true }

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _isInMealPlan = MutableStateFlow(false)
    val isInMealPlan: StateFlow<Boolean> = _isInMealPlan.asStateFlow()

    val cookbooks: StateFlow<List<Cookbook>> = cookbookDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadRecipe(id: String) = viewModelScope.launch {
        _recipe.value = recipeDao.getById(id)
        _isInMealPlan.value = mealPlanDao.existsByRecipeId(id)
    }

    fun toggleFavorite() = viewModelScope.launch {
        val r = _recipe.value ?: return@launch
        val updated = r.copy(favorited = !r.favorited)
        recipeDao.upsert(updated)
        _recipe.value = updated
    }

    fun updateRecipe(recipe: Recipe) = viewModelScope.launch {
        recipeDao.upsert(recipe)
        _recipe.value = recipe
    }

    fun deleteRecipe() = viewModelScope.launch {
        _recipe.value?.let { recipeDao.delete(it) }
    }

    fun addToMealPlan() = viewModelScope.launch {
        val r = _recipe.value ?: return@launch
        if (_isInMealPlan.value) return@launch
        val item = MealPlanItem(
            id = "mp_${System.currentTimeMillis()}",
            recipeId = r.id,
            servings = r.servings
        )
        mealPlanDao.upsert(item)
        _isInMealPlan.value = true
    }

    fun addToCookbook(cookbookId: String) = viewModelScope.launch {
        val r = _recipe.value ?: return@launch
        val cookbook = cookbookDao.getById(cookbookId) ?: return@launch
        val ids = cookbook.recipeIdsJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        }?.toMutableList() ?: mutableListOf()
        if (r.id !in ids) {
            ids.add(r.id)
            cookbookDao.upsert(cookbook.copy(recipeIdsJson = json.encodeToString<List<String>>(ids)))
        }
    }

    fun createCookbook(title: String) = viewModelScope.launch {
        cookbookDao.upsert(Cookbook(id = java.util.UUID.randomUUID().toString(), title = title))
    }
}
