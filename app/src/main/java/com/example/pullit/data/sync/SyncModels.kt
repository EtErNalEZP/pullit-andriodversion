package com.example.pullit.data.sync

import com.example.pullit.data.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

@Serializable
data class SupabaseRecipe(
    val id: String,
    val title: String,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    val description: String? = null,
    @SerialName("cook_time") val cookTime: String? = null,
    val calories: String? = null,
    val servings: Int = 2,
    val nutrition: String? = null,
    val ingredients: String? = null,
    val steps: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val transcription: String? = null,
    val labels: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("added_at") val addedAt: String? = null,
    val favorited: Boolean = false
) {
    fun toLocal(): Recipe = Recipe(
        id = id, title = title, imageUrl = imageUrl, videoUrl = videoUrl,
        desc = description, cookTime = cookTime, calories = calories,
        servings = servings, nutritionJson = nutrition, ingredientsJson = ingredients,
        stepsJson = steps, sourceUrl = sourceUrl, transcription = transcription,
        labelsJson = labels, favorited = favorited,
        createdAt = createdAt?.let { runCatching { isoFormatter.parse(it)?.time }.getOrNull() } ?: System.currentTimeMillis(),
        addedAt = addedAt?.let { runCatching { isoFormatter.parse(it)?.time }.getOrNull() } ?: System.currentTimeMillis()
    )

    companion object {
        fun fromLocal(recipe: Recipe, userId: String) = SupabaseRecipe(
            id = recipe.id, title = recipe.title, userId = userId,
            imageUrl = recipe.imageUrl, videoUrl = recipe.videoUrl,
            description = recipe.desc, cookTime = recipe.cookTime,
            calories = recipe.calories, servings = recipe.servings,
            nutrition = recipe.nutritionJson, ingredients = recipe.ingredientsJson,
            steps = recipe.stepsJson, sourceUrl = recipe.sourceUrl,
            transcription = recipe.transcription, labels = recipe.labelsJson,
            createdAt = isoFormatter.format(Date(recipe.createdAt)),
            addedAt = isoFormatter.format(Date(recipe.addedAt)),
            favorited = recipe.favorited
        )
    }
}

@Serializable
data class SupabaseCookbook(
    val id: String,
    val title: String,
    @SerialName("user_id") val userId: String,
    val tags: String? = null
) {
    fun toLocal(): Cookbook = Cookbook(id = id, title = title, tagsJson = tags)

    companion object {
        fun fromLocal(cookbook: Cookbook, userId: String) = SupabaseCookbook(
            id = cookbook.id, title = cookbook.title, userId = userId, tags = cookbook.tagsJson
        )
    }
}

@Serializable
data class SupabaseCookbookRecipe(
    @SerialName("cookbook_id") val cookbookId: String,
    @SerialName("recipe_id") val recipeId: String
)
