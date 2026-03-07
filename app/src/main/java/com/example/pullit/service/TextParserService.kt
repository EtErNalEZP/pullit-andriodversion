package com.example.pullit.service

import com.example.pullit.data.model.*
import com.example.pullit.util.AppLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class ParseRecipeRequest(val text: String, val format: String, val language: String)

object TextParserService {
    suspend fun parseText(text: String): ParsedRecipeResponse {
        return ApiClient.postTyped("/api/parse-recipe", ParseRecipeRequest(text = text, format = "json", language = AppLanguage.resolved))
    }

    fun convertToRecipe(parsed: ParsedRecipeResponse, id: String = UUID.randomUUID().toString()): Recipe {
        val ingredientsJson = ApiClient.json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Ingredient.serializer()),
            (parsed.ingredients ?: emptyList()).map {
                Ingredient(name = it.name ?: "", amount = it.amount ?: "", section = it.section)
            }
        )
        val stepsJson = ApiClient.json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Step.serializer()),
            (parsed.steps ?: emptyList()).mapIndexed { index, step ->
                Step(order = index + 1, instruction = step.instruction ?: "")
            }
        )
        return Recipe(
            id = id,
            title = parsed.title ?: "Untitled",
            desc = parsed.description,
            cookTime = parsed.cookTime,
            calories = parsed.calories,
            ingredientsJson = ingredientsJson,
            stepsJson = stepsJson
        )
    }
}
