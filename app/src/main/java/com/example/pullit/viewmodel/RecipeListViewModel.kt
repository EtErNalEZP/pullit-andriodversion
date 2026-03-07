package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.Cookbook
import com.example.pullit.data.model.Recipe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class SortOrder { NEWEST, OLDEST, NAME }

class RecipeListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val recipeDao = db.recipeDao()
    private val cookbookDao = db.cookbookDao()
    private val json = Json { ignoreUnknownKeys = true }

    val recipes: StateFlow<List<Recipe>> = recipeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cookbooks: StateFlow<List<Cookbook>> = cookbookDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedCookbookId = MutableStateFlow<String?>(null)
    val selectedCookbookId: StateFlow<String?> = _selectedCookbookId.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedRecipeIds: StateFlow<Set<String>> = _selectedRecipeIds.asStateFlow()

    val filteredRecipes: StateFlow<List<Recipe>> = combine(
        recipes, _searchQuery, _sortOrder, _selectedCookbookId, _showFavoritesOnly
    ) { allRecipes, query, sort, cookbookId, favsOnly ->
        var result = allRecipes

        if (favsOnly) result = result.filter { it.favorited }

        if (cookbookId != null) {
            val cookbook = cookbooks.value.find { it.id == cookbookId }
            val recipeIds = cookbook?.recipeIdsJson?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            result = result.filter { it.id in recipeIds }
        }

        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter { it.title.lowercase().contains(q) || it.desc?.lowercase()?.contains(q) == true }
        }

        when (sort) {
            SortOrder.NEWEST -> result.sortedByDescending { it.addedAt }
            SortOrder.OLDEST -> result.sortedBy { it.addedAt }
            SortOrder.NAME -> result.sortedBy { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    fun setSelectedCookbook(id: String?) { _selectedCookbookId.value = id }
    fun toggleFavoritesOnly() { _showFavoritesOnly.value = !_showFavoritesOnly.value }
    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) _selectedRecipeIds.value = emptySet()
    }
    fun toggleRecipeSelection(id: String) {
        _selectedRecipeIds.value = if (id in _selectedRecipeIds.value)
            _selectedRecipeIds.value - id else _selectedRecipeIds.value + id
    }

    fun toggleFavorite(recipe: Recipe) = viewModelScope.launch {
        recipeDao.upsert(recipe.copy(favorited = !recipe.favorited))
    }

    fun deleteRecipe(recipe: Recipe) = viewModelScope.launch {
        recipeDao.delete(recipe)
    }

    fun deleteSelected() = viewModelScope.launch {
        _selectedRecipeIds.value.forEach { recipeDao.deleteById(it) }
        _selectedRecipeIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun createCookbook(title: String) = viewModelScope.launch {
        val cookbook = Cookbook(id = java.util.UUID.randomUUID().toString(), title = title)
        cookbookDao.upsert(cookbook)
    }

    fun deleteCookbook(cookbook: Cookbook) = viewModelScope.launch {
        cookbookDao.delete(cookbook)
    }

    fun addRecipeToCookbook(recipeId: String, cookbookId: String) = viewModelScope.launch {
        val cookbook = cookbookDao.getById(cookbookId) ?: return@launch
        val ids = cookbook.recipeIdsJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        }?.toMutableList() ?: mutableListOf()
        if (recipeId !in ids) {
            ids.add(recipeId)
            cookbookDao.upsert(cookbook.copy(recipeIdsJson = json.encodeToString<List<String>>(ids)))
        }
    }
}
