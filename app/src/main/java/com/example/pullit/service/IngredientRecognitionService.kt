package com.example.pullit.service

import com.example.pullit.data.model.*
import com.example.pullit.util.AppLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RecognizeIngredientsRequest(val image: String, val language: String)

@Serializable
private data class SuggestRecipesRequest(val ingredients: List<String>, val count: Int, val language: String)

object IngredientRecognitionService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun recognizeIngredients(imageBase64: String): List<RecognizedIngredient> {
        val response: IngredientRecognitionResponse = ApiClient.postTyped(
            "/api/recognize-ingredients",
            RecognizeIngredientsRequest(image = imageBase64, language = AppLanguage.resolved)
        )
        return response.ingredients
    }

    suspend fun suggestRecipes(ingredients: List<String>): List<RecipeSuggestion> {
        val response: RecipeSuggestionsResponse = ApiClient.postTyped(
            "/api/suggest-recipes",
            SuggestRecipesRequest(ingredients = ingredients, count = 3, language = AppLanguage.resolved)
        )
        return response.suggestions
    }

    fun matchLocalRecipes(ingredients: List<String>, recipes: List<Recipe>): List<RecipeRecommendation> {
        val ingredientSet = ingredients.map { it.lowercase() }.toSet()
        val recommendations = mutableListOf<RecipeRecommendation>()

        for (recipe in recipes) {
            val recipeIngredients: List<Ingredient> = recipe.ingredientsJson?.let {
                runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            if (recipeIngredients.isEmpty()) continue

            val recipeIngNames = recipeIngredients.map { it.name.lowercase() }
            val matched = mutableListOf<String>()
            val missing = mutableListOf<String>()

            for (ri in recipeIngNames) {
                if (ingredientSet.any { ui -> ri.contains(ui) || ui.contains(ri) }) {
                    matched.add(ri)
                } else {
                    missing.add(ri)
                }
            }

            val score = matched.size.toDouble() / recipeIngNames.size
            if (score >= 0.3) {
                recommendations.add(RecipeRecommendation(
                    id = recipe.id, title = recipe.title, description = recipe.desc ?: "",
                    matchedIngredients = matched, missingIngredients = missing, matchScore = score
                ))
            }
        }

        return recommendations.sortedByDescending { it.matchScore }.take(10)
    }
}
