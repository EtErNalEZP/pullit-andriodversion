package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MealPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val mealPlanDao = db.mealPlanDao()
    private val recipeDao = db.recipeDao()
    private val groceryDao = db.groceryDao()
    private val json = Json { ignoreUnknownKeys = true }

    val mealPlanItems: StateFlow<List<MealPlanItem>> = mealPlanDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<Recipe>> = recipeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groceryItems: StateFlow<List<GroceryItem>> = groceryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToMealPlan(recipeId: String, servings: Int = 1) = viewModelScope.launch {
        if (mealPlanDao.existsByRecipeId(recipeId)) return@launch
        mealPlanDao.upsert(MealPlanItem(
            id = "mp_${System.currentTimeMillis()}", recipeId = recipeId, servings = servings
        ))
    }

    fun removeFromMealPlan(item: MealPlanItem) = viewModelScope.launch { mealPlanDao.delete(item) }

    fun clearMealPlan() = viewModelScope.launch { mealPlanDao.deleteAll() }

    fun generateGroceryList() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            groceryDao.deleteAll()
            val items = mealPlanItems.value
            val recipeIds = items.map { it.recipeId }
            val allRecipes = recipes.value.associateBy { it.id }
            val ingredientMap = mutableMapOf<String, MutableList<String>>()

            for (item in items) {
                val recipe = allRecipes[item.recipeId] ?: continue
                val ingredients: List<Ingredient> = recipe.ingredientsJson?.let {
                    runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList())
                } ?: emptyList()

                for (ing in ingredients) {
                    val key = ing.name.lowercase().trim()
                    if (key.isNotBlank()) {
                        ingredientMap.getOrPut(key) { mutableListOf() }
                        ingredientMap[key]!!.add("${ing.amount} (${recipe.title})")
                    }
                }
            }

            val groceries = ingredientMap.map { (name, amounts) ->
                GroceryItem(
                    id = "gi_${System.currentTimeMillis()}_${name.hashCode()}",
                    name = name.replaceFirstChar { it.uppercase() },
                    amount = amounts.joinToString(", "),
                    fromRecipesJson = json.encodeToString<List<String>>(amounts)
                )
            }
            groceryDao.upsertAll(groceries)
        }
    }

    fun toggleGroceryChecked(item: GroceryItem) = viewModelScope.launch {
        groceryDao.upsert(item.copy(checked = !item.checked))
    }

    fun addCustomGroceryItem(name: String) = viewModelScope.launch {
        groceryDao.upsert(GroceryItem(
            id = "gi_${System.currentTimeMillis()}", name = name
        ))
    }

    fun clearCheckedGroceries() = viewModelScope.launch { groceryDao.deleteChecked() }

    fun clearAllGroceries() = viewModelScope.launch { groceryDao.deleteAll() }

    fun updateServings(item: MealPlanItem, newServings: Int) = viewModelScope.launch {
        if (newServings >= 1) {
            mealPlanDao.upsert(item.copy(servings = newServings))
        }
    }
}
