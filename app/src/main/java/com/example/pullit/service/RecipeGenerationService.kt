package com.example.pullit.service

import com.example.pullit.data.model.*
import com.example.pullit.util.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GenerateRecipeRequest(
    @SerialName("video_url") val videoUrl: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("is_text_note") val isTextNote: Boolean? = null,
    @SerialName("extracted_title") val extractedTitle: String? = null,
    @SerialName("extracted_description") val extractedDescription: String? = null,
    @SerialName("extracted_cover_url") val extractedCoverUrl: String? = null,
    val language: String? = null
)

object RecipeGenerationService {

    suspend fun generateRecipe(
        videoUrl: String,
        sourceUrl: String? = null,
        isTextNote: Boolean? = null,
        extractedTitle: String? = null,
        extractedDescription: String? = null,
        extractedCoverUrl: String? = null,
        onProgress: (RecipeGenerationProgress) -> Unit
    ): Recipe {
        onProgress(RecipeGenerationProgress(RecipeGenerationProgress.Status.PENDING, "Preparing..."))

        val request = GenerateRecipeRequest(
            videoUrl = videoUrl, sourceUrl = sourceUrl, isTextNote = isTextNote,
            extractedTitle = extractedTitle, extractedDescription = extractedDescription,
            extractedCoverUrl = extractedCoverUrl, language = AppLanguage.resolved
        )

        val creation: TaskCreationResponse = ApiClient.postTyped("/api/generate-recipe", request)
        val taskId = creation.taskId

        onProgress(RecipeGenerationProgress(RecipeGenerationProgress.Status.DOWNLOADING, "Downloading...", taskId = taskId))

        val startTime = System.currentTimeMillis()
        var attempts = 0

        while (attempts < 200) {
            if (System.currentTimeMillis() - startTime > 600_000) throw ApiError.Timeout

            if (attempts > 0) delay(3000)
            attempts++

            val status: TaskStatusResponse = ApiClient.get("/api/generate-recipe/$taskId")
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val progressStatus = mapState(status.state)

            onProgress(RecipeGenerationProgress(
                status = progressStatus,
                message = progressStatus.name,
                elapsedSeconds = elapsed,
                taskId = taskId
            ))

            if (status.state == "completed" && status.recipe != null) {
                onProgress(RecipeGenerationProgress(RecipeGenerationProgress.Status.COMPLETED, "Done!", taskId = taskId))
                return parseRecipe(status.recipe)
            } else if (status.state == "error") {
                throw ApiError.Unknown(status.error ?: "Recipe generation failed")
            }
        }
        throw ApiError.Timeout
    }

    private fun mapState(state: String): RecipeGenerationProgress.Status = when (state) {
        "pending" -> RecipeGenerationProgress.Status.PENDING
        "checking_content" -> RecipeGenerationProgress.Status.CHECKING_CONTENT
        "downloading" -> RecipeGenerationProgress.Status.DOWNLOADING
        "extracting_audio" -> RecipeGenerationProgress.Status.EXTRACTING_AUDIO
        "uploading" -> RecipeGenerationProgress.Status.UPLOADING
        "transcribing" -> RecipeGenerationProgress.Status.TRANSCRIBING
        "generating_recipe" -> RecipeGenerationProgress.Status.GENERATING_RECIPE
        "completed" -> RecipeGenerationProgress.Status.COMPLETED
        "error" -> RecipeGenerationProgress.Status.ERROR
        else -> RecipeGenerationProgress.Status.PENDING
    }

    fun parseRecipe(backend: BackendRecipe): Recipe {
        val ingredientsJson = ApiClient.json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Ingredient.serializer()),
            (backend.ingredients ?: emptyList()).map {
                Ingredient(name = it.name ?: "", amount = it.amount ?: "", section = it.section)
            }
        )
        val stepsJson = ApiClient.json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Step.serializer()),
            (backend.steps ?: emptyList()).map {
                Step(order = it.order ?: 0, instruction = it.instruction ?: "")
            }
        )
        val nutritionJson = backend.nutrition?.let {
            ApiClient.json.encodeToString(
                Nutrition.serializer(),
                Nutrition(calories = it.calories ?: "", protein = it.protein ?: "", fat = it.fat ?: "", carbs = it.carbs ?: "")
            )
        }
        val labelsJson = if (backend.cuisine != null || backend.mealType != null || !backend.tags.isNullOrEmpty()) {
            ApiClient.json.encodeToString(
                RecipeLabels.serializer(),
                RecipeLabels(cuisine = backend.cuisine, mealType = backend.mealType, tags = backend.tags ?: emptyList())
            )
        } else null

        val imageUrl = backend.imageBase64 ?: backend.imageUrl

        return Recipe(
            id = backend.id ?: UUID.randomUUID().toString(),
            title = backend.title ?: "Untitled Recipe",
            imageUrl = imageUrl,
            videoUrl = backend.videoUrl,
            desc = backend.description,
            cookTime = backend.cookTime,
            calories = backend.calories,
            servings = backend.servings ?: 2,
            nutritionJson = nutritionJson,
            ingredientsJson = ingredientsJson,
            stepsJson = stepsJson,
            sourceUrl = backend.sourceUrl,
            transcription = backend.transcription,
            labelsJson = labelsJson
        )
    }
}
