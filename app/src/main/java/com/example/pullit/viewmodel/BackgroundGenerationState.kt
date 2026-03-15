package com.example.pullit.viewmodel

import com.example.pullit.data.model.Recipe
import com.example.pullit.data.model.RecipeGenerationProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class GenerationTask(
    val id: String = UUID.randomUUID().toString(),
    val progress: RecipeGenerationProgress? = null,
    val pendingTitle: String? = null,
    val pendingCoverUrl: String? = null,
    val generatedRecipe: Recipe? = null,
    val errorMessage: String? = null
) {
    val isCompleted: Boolean
        get() = progress?.status == RecipeGenerationProgress.Status.COMPLETED && generatedRecipe != null
    val isError: Boolean
        get() = progress?.status == RecipeGenerationProgress.Status.ERROR
}

object BackgroundGenerationState {
    private val _tasks = MutableStateFlow<List<GenerationTask>>(emptyList())
    val tasks: StateFlow<List<GenerationTask>> = _tasks.asStateFlow()

    const val MAX_CONCURRENT = 2

    val activeTasks: List<GenerationTask>
        get() = _tasks.value.filter { !it.isCompleted && !it.isError }
    val canStartNew: Boolean
        get() = activeTasks.size < MAX_CONCURRENT
    val isGenerating: Boolean
        get() = _tasks.value.any { !it.isCompleted && !it.isError }

    private val _cookbookRecommendationRecipe = MutableStateFlow<Recipe?>(null)
    val cookbookRecommendationRecipe: StateFlow<Recipe?> = _cookbookRecommendationRecipe.asStateFlow()

    // Legacy single-task compatibility fields
    private val _progress = MutableStateFlow<RecipeGenerationProgress?>(null)
    val progress: StateFlow<RecipeGenerationProgress?> = _progress.asStateFlow()

    private val _previewTitle = MutableStateFlow<String?>(null)
    val previewTitle: StateFlow<String?> = _previewTitle.asStateFlow()

    private val _previewCoverUrl = MutableStateFlow<String?>(null)
    val previewCoverUrl: StateFlow<String?> = _previewCoverUrl.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun start(title: String? = null, coverUrl: String? = null): String {
        val task = GenerationTask(
            progress = RecipeGenerationProgress(
                status = RecipeGenerationProgress.Status.PENDING,
                message = "Pending..."
            ),
            pendingTitle = title,
            pendingCoverUrl = coverUrl
        )
        _tasks.value = listOf(task) + _tasks.value
        // Legacy compat
        _progress.value = task.progress
        _previewTitle.value = title
        _previewCoverUrl.value = coverUrl
        _error.value = null
        return task.id
    }

    fun updateProgress(id: String, progress: RecipeGenerationProgress) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(progress = progress) else it
        }
        // Legacy compat
        _progress.value = progress
    }

    // Legacy overload for existing callers
    fun updateProgress(progress: RecipeGenerationProgress) {
        _progress.value = progress
        // Also update the first active task
        val firstActive = _tasks.value.firstOrNull { !it.isCompleted && !it.isError }
        if (firstActive != null) {
            _tasks.value = _tasks.value.map {
                if (it.id == firstActive.id) it.copy(progress = progress) else it
            }
        }
    }

    fun updatePendingInfo(id: String, title: String?, coverUrl: String?) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(
                pendingTitle = title?.takeIf { t -> t.isNotEmpty() } ?: it.pendingTitle,
                pendingCoverUrl = coverUrl?.takeIf { c -> c.isNotEmpty() } ?: it.pendingCoverUrl
            ) else it
        }
        // Legacy compat
        if (title != null) _previewTitle.value = title
        if (coverUrl != null) _previewCoverUrl.value = coverUrl
    }

    fun setPreview(title: String?, coverUrl: String?) {
        _previewTitle.value = title
        _previewCoverUrl.value = coverUrl
        // Also update the first active task
        val firstActive = _tasks.value.firstOrNull { !it.isCompleted && !it.isError }
        if (firstActive != null) {
            updatePendingInfo(firstActive.id, title, coverUrl)
        }
    }

    fun complete(id: String, recipe: Recipe, autoCookbookRecommend: Boolean = true) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(
                generatedRecipe = recipe,
                progress = RecipeGenerationProgress(
                    status = RecipeGenerationProgress.Status.COMPLETED,
                    message = "Completed"
                )
            ) else it
        }
        if (autoCookbookRecommend) {
            _cookbookRecommendationRecipe.value = recipe
        }
    }

    fun fail(id: String, message: String) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(
                errorMessage = message,
                progress = RecipeGenerationProgress(
                    status = RecipeGenerationProgress.Status.ERROR,
                    message = message
                )
            ) else it
        }
        // Legacy compat
        _error.value = message
    }

    fun setError(message: String?) {
        _error.value = message
        if (message != null) {
            val firstActive = _tasks.value.firstOrNull { !it.isCompleted && !it.isError }
            if (firstActive != null) {
                fail(firstActive.id, message)
            }
        }
    }

    fun remove(id: String) {
        _tasks.value = _tasks.value.filter { it.id != id }
    }

    fun clearRecommendation() {
        _cookbookRecommendationRecipe.value = null
    }

    fun reset() {
        _tasks.value = emptyList()
        _progress.value = null
        _previewTitle.value = null
        _previewCoverUrl.value = null
        _error.value = null
        _cookbookRecommendationRecipe.value = null
    }
}
