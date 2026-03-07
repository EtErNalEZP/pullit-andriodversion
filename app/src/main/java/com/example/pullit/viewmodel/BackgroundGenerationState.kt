package com.example.pullit.viewmodel

import com.example.pullit.data.model.RecipeGenerationProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BackgroundGenerationState {
    private val _progress = MutableStateFlow<RecipeGenerationProgress?>(null)
    val progress: StateFlow<RecipeGenerationProgress?> = _progress.asStateFlow()

    private val _previewTitle = MutableStateFlow<String?>(null)
    val previewTitle: StateFlow<String?> = _previewTitle.asStateFlow()

    private val _previewCoverUrl = MutableStateFlow<String?>(null)
    val previewCoverUrl: StateFlow<String?> = _previewCoverUrl.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateProgress(progress: RecipeGenerationProgress) { _progress.value = progress }
    fun setPreview(title: String?, coverUrl: String?) { _previewTitle.value = title; _previewCoverUrl.value = coverUrl }
    fun setError(message: String?) { _error.value = message }

    fun reset() {
        _progress.value = null
        _previewTitle.value = null
        _previewCoverUrl.value = null
        _error.value = null
    }
}
