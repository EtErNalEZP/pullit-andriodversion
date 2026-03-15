package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.Cookbook
import com.example.pullit.data.model.Nutrition
import com.example.pullit.data.model.Recipe
import com.example.pullit.data.model.RecipeLabels
import com.example.pullit.ui.AppStrings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class SortOrder { NEWEST, OLDEST, NAME }

enum class CookTimeFilter {
    ANY, UNDER_15, UNDER_30, UNDER_60, OVER_60;

    fun label(S: AppStrings): String = when (this) {
        ANY -> S.filterAny
        UNDER_15 -> S.filterUnder15
        UNDER_30 -> S.filterUnder30
        UNDER_60 -> S.filterUnder60
        OVER_60 -> S.filterOver60
    }

    fun matches(minutes: Int): Boolean = when (this) {
        ANY -> true
        UNDER_15 -> minutes <= 15
        UNDER_30 -> minutes <= 30
        UNDER_60 -> minutes <= 60
        OVER_60 -> minutes > 60
    }
}

enum class CalorieFilter {
    ANY, UNDER_300, UNDER_500, UNDER_800, OVER_800;

    fun label(S: AppStrings): String = when (this) {
        ANY -> S.filterAny
        UNDER_300 -> S.filterUnder300
        UNDER_500 -> S.filterUnder500
        UNDER_800 -> S.filterUnder800
        OVER_800 -> S.filterOver800
    }

    fun matches(kcal: Int): Boolean = when (this) {
        ANY -> true
        UNDER_300 -> kcal <= 300
        UNDER_500 -> kcal <= 500
        UNDER_800 -> kcal <= 800
        OVER_800 -> kcal > 800
    }
}

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

    private val _cookTimeFilter = MutableStateFlow(CookTimeFilter.ANY)
    val cookTimeFilter: StateFlow<CookTimeFilter> = _cookTimeFilter.asStateFlow()

    private val _calorieFilter = MutableStateFlow(CalorieFilter.ANY)
    val calorieFilter: StateFlow<CalorieFilter> = _calorieFilter.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _showFilterPanel = MutableStateFlow(false)
    val showFilterPanel: StateFlow<Boolean> = _showFilterPanel.asStateFlow()

    val availableTags: StateFlow<List<String>> = recipes.map { allRecipes ->
        val counts = mutableMapOf<String, Int>()
        for (recipe in allRecipes) {
            val labels = recipe.labelsJson?.let {
                runCatching { json.decodeFromString<RecipeLabels>(it) }.getOrNull()
            } ?: continue
            for (value in labels.allValues) {
                counts[value] = (counts[value] ?: 0) + 1
            }
        }
        counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _cookTimeFilter, _calorieFilter, _selectedTags
    ) { ct, cal, tags ->
        ct != CookTimeFilter.ANY || cal != CalorieFilter.ANY || tags.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private data class BaseFilters(
        val recipes: List<Recipe>,
        val query: String,
        val sort: SortOrder,
        val cookbookId: String?,
        val favsOnly: Boolean
    )

    val filteredRecipes: StateFlow<List<Recipe>> = combine(
        combine(recipes, _searchQuery, _sortOrder, _selectedCookbookId, _showFavoritesOnly, ::BaseFilters),
        _cookTimeFilter,
        _calorieFilter,
        _selectedTags
    ) { base, cookTimeFilter, calorieFilter, selectedTags ->
        var result = base.recipes

        if (base.favsOnly) result = result.filter { it.favorited }

        if (base.cookbookId != null) {
            val cookbook = cookbooks.value.find { it.id == base.cookbookId }
            val recipeIds = cookbook?.recipeIdsJson?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            result = result.filter { it.id in recipeIds }
        }

        // Cook time filter
        if (cookTimeFilter != CookTimeFilter.ANY) {
            result = result.filter { recipe ->
                val minutes = parseCookTimeMinutes(recipe.cookTime) ?: return@filter false
                cookTimeFilter.matches(minutes)
            }
        }

        // Calorie filter
        if (calorieFilter != CalorieFilter.ANY) {
            result = result.filter { recipe ->
                val nutrition = recipe.nutritionJson?.let {
                    runCatching { json.decodeFromString<Nutrition>(it) }.getOrNull()
                }
                val kcal = if (nutrition != null) {
                    parseCalories(nutrition.calories)
                } else {
                    parseCalories(recipe.calories)
                } ?: return@filter false
                calorieFilter.matches(kcal)
            }
        }

        // Tag filter
        if (selectedTags.isNotEmpty()) {
            val lowered = selectedTags.map { it.lowercase() }.toSet()
            result = result.filter { recipe ->
                val labels = recipe.labelsJson?.let {
                    runCatching { json.decodeFromString<RecipeLabels>(it) }.getOrNull()
                } ?: return@filter false
                val recipeValues = labels.allValues.map { it.lowercase() }.toSet()
                recipeValues.intersect(lowered).isNotEmpty()
            }
        }

        // Search filter
        if (base.query.isNotBlank()) {
            val q = base.query.lowercase()
            result = result.filter { it.title.lowercase().contains(q) || it.desc?.lowercase()?.contains(q) == true }
        }

        when (base.sort) {
            SortOrder.NEWEST -> result.sortedByDescending { it.addedAt }
            SortOrder.OLDEST -> result.sortedBy { it.addedAt }
            SortOrder.NAME -> result.sortedBy { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    fun setSelectedCookbook(id: String?) { _selectedCookbookId.value = id }
    fun toggleFavoritesOnly() { _showFavoritesOnly.value = !_showFavoritesOnly.value }
    fun setCookTimeFilter(filter: CookTimeFilter) { _cookTimeFilter.value = filter }
    fun setCalorieFilter(filter: CalorieFilter) { _calorieFilter.value = filter }
    fun toggleTag(tag: String) {
        _selectedTags.value = if (tag in _selectedTags.value)
            _selectedTags.value - tag else _selectedTags.value + tag
    }
    fun clearFilters() {
        _cookTimeFilter.value = CookTimeFilter.ANY
        _calorieFilter.value = CalorieFilter.ANY
        _selectedTags.value = emptySet()
    }
    fun toggleFilterPanel() { _showFilterPanel.value = !_showFilterPanel.value }
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

    fun removeRecipeFromCookbook(recipeId: String, cookbookId: String) = viewModelScope.launch {
        val cookbook = cookbookDao.getById(cookbookId) ?: return@launch
        val ids = cookbook.recipeIdsJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        }?.toMutableList() ?: mutableListOf()
        ids.remove(recipeId)
        cookbookDao.upsert(cookbook.copy(recipeIdsJson = json.encodeToString<List<String>>(ids)))
    }

    fun recipesForCookbook(cookbook: Cookbook): List<Recipe> {
        val recipeIds = cookbook.recipeIdsJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
        return recipes.value.filter { it.id in recipeIds }
    }

    fun updateRecipeLabels(recipeId: String, labels: RecipeLabels) = viewModelScope.launch {
        val recipe = recipeDao.getById(recipeId) ?: return@launch
        val labelsJson = json.encodeToString(RecipeLabels.serializer(), labels)
        recipeDao.upsert(recipe.copy(labelsJson = labelsJson))
    }

    companion object {
        fun parseCookTimeMinutes(cookTime: String?): Int? {
            val text = cookTime?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
            var total = 0
            Regex("""(\d+)\s*h""").find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 60 }
            Regex("""(\d+)\s*m""").find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { total += it }
            if (total == 0) {
                text.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }?.let { total = it }
            }
            return if (total > 0) total else null
        }

        fun parseCalories(calories: String?): Int? {
            val text = calories?.takeIf { it.isNotEmpty() } ?: return null
            return Regex("""(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
        }
    }

    fun createCookbookWithTags(title: String, recipeId: String, tags: List<String>) = viewModelScope.launch {
        val cookbook = Cookbook(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            recipeIdsJson = json.encodeToString<List<String>>(listOf(recipeId)),
            tagsJson = if (tags.isNotEmpty()) json.encodeToString<List<String>>(tags) else null
        )
        cookbookDao.upsert(cookbook)
    }
}
