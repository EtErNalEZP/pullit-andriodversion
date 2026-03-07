package com.example.pullit.data.local

import androidx.room.TypeConverter
import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Nutrition
import com.example.pullit.data.model.RecipeLabels
import com.example.pullit.data.model.Step
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun ingredientsToJson(value: List<Ingredient>): String = json.encodeToString(value)
    @TypeConverter
    fun jsonToIngredients(value: String): List<Ingredient> =
        runCatching { json.decodeFromString<List<Ingredient>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun stepsToJson(value: List<Step>): String = json.encodeToString(value)
    @TypeConverter
    fun jsonToSteps(value: String): List<Step> =
        runCatching { json.decodeFromString<List<Step>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun nutritionToJson(value: Nutrition?): String? = value?.let { json.encodeToString(it) }
    @TypeConverter
    fun jsonToNutrition(value: String?): Nutrition? =
        value?.let { runCatching { json.decodeFromString<Nutrition>(it) }.getOrNull() }

    @TypeConverter
    fun labelsToJson(value: RecipeLabels?): String? = value?.let { json.encodeToString(it) }
    @TypeConverter
    fun jsonToLabels(value: String?): RecipeLabels? =
        value?.let { runCatching { json.decodeFromString<RecipeLabels>(it) }.getOrNull() }

    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(value)
    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}
