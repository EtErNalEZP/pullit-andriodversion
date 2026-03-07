package com.example.pullit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull

@Serializable
data class BackendNutrition(
    val calories: String? = null,
    val protein: String? = null,
    val fat: String? = null,
    val carbs: String? = null
)

@Serializable
data class BackendRecipe(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("cook_time") val cookTime: String? = null,
    val calories: String? = null,
    val servings: Int? = null,
    val nutrition: BackendNutrition? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_base64") val imageBase64: String? = null,
    val ingredients: List<BackendIngredient>? = null,
    val steps: List<BackendStep>? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val transcription: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val cuisine: String? = null,
    @SerialName("meal_type") val mealType: String? = null,
    val tags: List<String>? = null
)

@Serializable
data class BackendIngredient(
    val name: String? = null,
    val amount: String? = null,
    val section: String? = null
)

@Serializable
data class BackendStep(
    val order: Int? = null,
    val instruction: String? = null
)

@Serializable
data class TaskCreationResponse(
    @SerialName("task_id") val taskId: String,
    val message: String
)

@Serializable
data class TaskStatusResponse(
    val state: String,
    val progress: JsonElement? = null,
    val recipe: BackendRecipe? = null,
    val error: String? = null,
    @SerialName("elapsed_seconds") val elapsedSeconds: Int? = null,
    val text: String? = null
) {
    val progressPercent: Double?
        get() = when (progress) {
            is JsonPrimitive -> progress.doubleOrNull ?: progress.intOrNull?.toDouble()
            else -> null
        }
}

@Serializable
data class OCRResult(
    val text: String? = null,
    val confidence: Double? = null
)

@Serializable
data class ParsedRecipeResponse(
    val title: String? = null,
    val description: String? = null,
    val ingredients: List<BackendIngredient>? = null,
    val steps: List<BackendStep>? = null,
    @SerialName("cook_time") val cookTime: String? = null,
    val calories: String? = null,
    val servings: Int? = null,
    val nutrition: BackendNutrition? = null
)

@Serializable
data class RecognizedIngredient(
    val name: String,
    val confidence: Double? = null,
    val quantity: String? = null
)

@Serializable
data class IngredientRecognitionResponse(
    val ingredients: List<RecognizedIngredient>
)

@Serializable
data class RecipeSuggestion(
    val title: String,
    val description: String,
    val calories: String? = null,
    val ingredients: List<String>,
    val steps: List<String>
)

@Serializable
data class RecipeSuggestionsResponse(
    val suggestions: List<RecipeSuggestion>
)

data class RecipeRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>,
    val matchScore: Double
)
