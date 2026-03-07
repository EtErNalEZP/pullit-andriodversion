package com.example.pullit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.data.model.*
import com.example.pullit.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

enum class ImportMethod { LINK, TEXT, IMAGE }

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val recipeDao = db.recipeDao()
    private val json = Json { ignoreUnknownKeys = true }

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _importMethod = MutableStateFlow(ImportMethod.LINK)
    val importMethod: StateFlow<ImportMethod> = _importMethod.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _progress = MutableStateFlow<RecipeGenerationProgress?>(null)
    val progress: StateFlow<RecipeGenerationProgress?> = _progress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _generatedRecipe = MutableStateFlow<Recipe?>(null)
    val generatedRecipe: StateFlow<Recipe?> = _generatedRecipe.asStateFlow()

    fun setInputText(text: String) { _inputText.value = text }
    fun setImportMethod(method: ImportMethod) { _importMethod.value = method }
    fun clearError() { _error.value = null }

    fun detectPlatform(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("xiaohongshu") || lower.contains("xhslink") -> "xiaohongshu"
            lower.contains("douyin") || lower.contains("v.douyin") -> "douyin"
            lower.contains("bilibili") || lower.contains("b23.tv") -> "bilibili"
            lower.contains("xiachufang") -> "xiachufang"
            lower.contains("tiktok") -> "tiktok"
            lower.contains("instagram") -> "instagram"
            lower.contains("youtube") || lower.contains("youtu.be") -> "youtube"
            else -> "unknown"
        }
    }

    fun importFromLink() = viewModelScope.launch {
        val text = _inputText.value.trim()
        if (text.isBlank()) return@launch

        _isGenerating.value = true
        _error.value = null
        _generatedRecipe.value = null

        try {
            val platform = detectPlatform(text)

            when (platform) {
                "xiachufang" -> importFromXiachufang(text)
                "xiaohongshu" -> importFromXiaohongshu(text)
                else -> importFromVideo(text)
            }
        } catch (e: Exception) {
            _error.value = when (e) {
                is ApiError.HttpError -> "HTTP ${e.code}: ${e.msg}"
                is ApiError.NetworkError -> "Network error: ${e.cause?.message}"
                is ApiError.DecodingError -> "Decode error: ${e.cause?.message}"
                is ApiError.Timeout -> "Request timed out"
                else -> "${e::class.simpleName}: ${e.message}"
            }
        } finally {
            _isGenerating.value = false
        }
    }

    private suspend fun importFromXiachufang(text: String) {
        val url = XiachufangService.extractUrl(text) ?: text
        val result = XiachufangService.parseUrl(url)

        val ingredientsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Ingredient.serializer()), result.ingredients
        )
        val stepsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Step.serializer()), result.steps
        )

        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = result.title,
            desc = result.description,
            imageUrl = result.imageUrl,
            videoUrl = result.videoUrl,
            cookTime = result.cookTime,
            ingredientsJson = ingredientsJson,
            stepsJson = stepsJson,
            sourceUrl = result.sourceUrl
        )
        recipeDao.upsert(recipe)
        _generatedRecipe.value = recipe
    }

    private suspend fun importFromXiaohongshu(text: String) {
        val content = XiaohongshuService.fetchContent(text)

        if (content.useBackendDirectly || content.videoUrl != null) {
            val recipe = RecipeGenerationService.generateRecipe(
                videoUrl = content.videoUrl ?: content.fullUrl,
                sourceUrl = content.fullUrl,
                isTextNote = content.isTextNote,
                extractedTitle = content.title.takeIf { it.isNotBlank() },
                extractedDescription = content.description.takeIf { it.isNotBlank() },
                extractedCoverUrl = content.coverUrl,
                onProgress = { _progress.value = it }
            )
            recipeDao.upsert(recipe)
            _generatedRecipe.value = recipe
        } else {
            val recipe = RecipeGenerationService.generateRecipe(
                videoUrl = content.fullUrl,
                sourceUrl = content.fullUrl,
                isTextNote = true,
                extractedTitle = content.title.takeIf { it.isNotBlank() },
                extractedDescription = content.description.takeIf { it.isNotBlank() },
                extractedCoverUrl = content.coverUrl,
                onProgress = { _progress.value = it }
            )
            recipeDao.upsert(recipe)
            _generatedRecipe.value = recipe
        }
    }

    private suspend fun importFromVideo(text: String) {
        val urlRegex = Regex("https?://[^\\s]+")
        val url = urlRegex.find(text)?.value ?: text

        val recipe = RecipeGenerationService.generateRecipe(
            videoUrl = url,
            onProgress = { _progress.value = it }
        )
        recipeDao.upsert(recipe)
        _generatedRecipe.value = recipe
    }

    fun importFromText(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        _isGenerating.value = true
        _error.value = null

        try {
            val parsed = TextParserService.parseText(text)
            val recipe = TextParserService.convertToRecipe(parsed)
            recipeDao.upsert(recipe)
            _generatedRecipe.value = recipe
        } catch (e: Exception) {
            _error.value = e.message ?: "Parse failed"
        } finally {
            _isGenerating.value = false
        }
    }

    fun importFromImage(imageData: ByteArray) = viewModelScope.launch {
        _isGenerating.value = true
        _error.value = null

        try {
            val parsed = OcrService.extractRecipe(imageData)
            val recipe = TextParserService.convertToRecipe(parsed)
            recipeDao.upsert(recipe)
            _generatedRecipe.value = recipe
        } catch (e: Exception) {
            _error.value = e.message ?: "Image import failed"
        } finally {
            _isGenerating.value = false
        }
    }

    fun reset() {
        _inputText.value = ""
        _isGenerating.value = false
        _progress.value = null
        _error.value = null
        _generatedRecipe.value = null
    }
}
