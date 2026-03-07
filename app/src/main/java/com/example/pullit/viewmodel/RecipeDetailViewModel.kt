package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.MealPlanItem
import com.example.pullit.data.model.Recipe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val recipeDao = db.recipeDao()
    private val mealPlanDao = db.mealPlanDao()

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    fun loadRecipe(id: String) = viewModelScope.launch {
        _recipe.value = recipeDao.getById(id)
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
        val item = MealPlanItem(
            id = "mp_${System.currentTimeMillis()}",
            recipeId = r.id,
            servings = r.servings
        )
        mealPlanDao.upsert(item)
    }
}
