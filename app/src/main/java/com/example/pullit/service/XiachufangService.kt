package com.example.pullit.service

import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Step
import kotlinx.serialization.Serializable

data class XiachufangRecipe(
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
    val sourceUrl: String,
    val cookTime: String? = null
)

@Serializable
private data class ParseXiachufangRequest(val url: String)

@Serializable
private data class ParseXiachufangResponse(
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val ingredients: List<ParsedIngredient>? = null,
    val steps: List<ParsedStep>? = null,
    val cookTime: String? = null,
    val sourceUrl: String? = null
) {
    @Serializable
    data class ParsedIngredient(val name: String, val amount: String)
    @Serializable
    data class ParsedStep(val order: Int, val instruction: String)
}

object XiachufangService {
    fun isXiachufangUrl(url: String): Boolean = url.lowercase().contains("xiachufang.com/recipe/")

    fun extractUrl(text: String): String? {
        val match = Regex("https?://(?:www\\.)?xiachufang\\.com/recipe/\\d+/?", RegexOption.IGNORE_CASE).find(text)
        return match?.value
    }

    suspend fun parseUrl(url: String): XiachufangRecipe {
        if (!isXiachufangUrl(url)) throw ApiError.Unknown("Not a valid Xiachufang URL")
        val response: ParseXiachufangResponse = ApiClient.postTyped("/api/parse-xiachufang", ParseXiachufangRequest(url))
        return XiachufangRecipe(
            title = response.title,
            description = response.description,
            imageUrl = response.imageUrl,
            videoUrl = response.videoUrl,
            ingredients = (response.ingredients ?: emptyList()).map { Ingredient(name = it.name, amount = it.amount) },
            steps = (response.steps ?: emptyList()).map { Step(order = it.order, instruction = it.instruction) },
            sourceUrl = response.sourceUrl ?: url,
            cookTime = response.cookTime
        )
    }
}
