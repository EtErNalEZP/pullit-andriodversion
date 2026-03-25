package com.example.pullit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val name: String,
    val amount: String = "",
    val section: String? = null
)

@Serializable
data class Step(
    val order: Int,
    val instruction: String,
    val imageUrls: List<String> = emptyList()
)

@Serializable
data class Nutrition(
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = ""
)

@Serializable
data class RecipeLabels(
    val cuisine: String? = null,
    val mealType: String? = null,
    val tags: List<String> = emptyList()
) {
    val allValues: List<String>
        get() {
            val seen = mutableSetOf<String>()
            val values = mutableListOf<String>()
            cuisine?.takeIf { it.isNotEmpty() }?.let {
                seen.add(it.lowercase())
                values.add(it)
            }
            mealType?.takeIf { it.isNotEmpty() }?.let {
                val key = it.lowercase()
                if (key !in seen) { seen.add(key); values.add(it) }
            }
            tags.forEach {
                val key = it.lowercase()
                if (key !in seen) { seen.add(key); values.add(it) }
            }
            return values
        }

    val isEmpty: Boolean get() = allValues.isEmpty()
}

// Progress tracking for recipe generation
data class RecipeGenerationProgress(
    val status: Status,
    val message: String,
    val elapsedSeconds: Int = 0,
    val taskId: String? = null
) {
    enum class Status {
        PENDING, CHECKING_CONTENT, DOWNLOADING, EXTRACTING_AUDIO,
        UPLOADING, TRANSCRIBING, GENERATING_RECIPE, COMPLETED, ERROR
    }
}

// Progress tracking for transcription
data class TranscriptionProgress(
    val status: Status,
    val message: String,
    val elapsedSeconds: Int = 0,
    val taskId: String? = null
) {
    enum class Status { CREATING, POLLING, SUCCESS, ERROR }
}
